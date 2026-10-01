package com.example.csc490seniorproject.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.csc490seniorproject.objects.Instrument
import com.example.csc490seniorproject.objects.Song
import com.example.csc490seniorproject.objects.Track
import com.example.csc490seniorproject.objects.endStep
import com.example.csc490seniorproject.objects.stepDurationMs
import com.example.csc490seniorproject.objects.totalSteps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

private const val SAMPLE_RATE = 44100

class PlaybackEngine(private val scope: CoroutineScope) {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _seekRequest = MutableStateFlow<Int?>(null)

    private var job: Job? = null

    fun play(song: Song) {
        stop() // guard against a second play() stacking a second render loop
        if (song.tracks.isEmpty() || song.totalSteps() <= 0) return
        _isPlaying.value = true

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf.coerceAtLeast(4096) * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack.play()

        job = scope.launch(Dispatchers.Default) {
            try {
                renderLoop(song, audioTrack)
            } finally {
                audioTrack.stop()
                audioTrack.release()
            }
        }
    }

    fun stop() {
        _isPlaying.value = false
        job?.cancel()
        job = null
    }

    fun seekTo(step: Int, totalSteps: Int) {
        val clamped = step.coerceIn(0, (totalSteps - 1).coerceAtLeast(0))
        _currentStep.value = clamped
        if (_isPlaying.value) _seekRequest.value = clamped
    }

    fun previewNote(instrument: Instrument, pitch: Int, velocity: Int = 100) {
        scope.launch(Dispatchers.Default) {
            val durationSec = 0.35
            val totalSamples = (durationSec * SAMPLE_RATE).toInt().coerceAtLeast(1)
            val buffer = ShortArray(totalSamples)
            for (i in 0 until totalSamples) {
                val sample = synthesize(
                    instrument = instrument,
                    pitch = pitch,
                    velocity = velocity,
                    sampleIndexInNote = i.toLong(),
                    noteDurationSamples = totalSamples.toLong()
                )
                buffer[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }

            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val previewTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes((buffer.size * 2).coerceAtLeast(minBuf))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            previewTrack.write(buffer, 0, buffer.size)
            previewTrack.play()
            delay((durationSec * 1000).toLong() + 50)
            previewTrack.stop()
            previewTrack.release()
        }
    }

    private suspend fun renderLoop(song: Song, audioTrack: AudioTrack) {
        val stepMs = song.stepDurationMs()
        val totalSteps = song.totalSteps()
        val samplesPerStep = (stepMs / 1000.0 * SAMPLE_RATE).toInt().coerceAtLeast(1)
        val chunkSize = 1024 // samples per write; small enough for low latency, big enough not to starve the device buffer
        val buffer = ShortArray(chunkSize)

        // Solo overrides mute: if ANY track is soloed, only soloed tracks are heard.
        val anySolo = song.tracks.any { it.solo }
        fun isAudible(t: Track) = if (anySolo) t.solo else !t.muted

        var globalSample = _currentStep.value.toLong() * samplesPerStep // resume from wherever the playhead is

        while (scope.isActive && _isPlaying.value) {
            _seekRequest.value?.let { seekStep ->
                globalSample = seekStep.toLong() * samplesPerStep
                _seekRequest.value = null
            }

            var i = 0
            while (i < chunkSize) {
                val step = ((globalSample / samplesPerStep.toDouble()).toInt()) % totalSteps
                if (step != _currentStep.value) _currentStep.value = step

                var mixed = 0f
                for (track in song.tracks) {
                    if (!isAudible(track)) continue
                    for (note in track.notes) {
                        if (step < note.startStep || step >= note.endStep()) continue

                        val stepsIntoNote = step - note.startStep
                        val sampleWithinStep = globalSample % samplesPerStep
                        val sampleIndexInNote = stepsIntoNote.toLong() * samplesPerStep + sampleWithinStep
                        val noteDurationSamples = note.lengthSteps.toLong() * samplesPerStep

                        mixed += synthesize(
                            instrument = track.instrument,
                            pitch = note.pitch,
                            velocity = note.velocity,
                            sampleIndexInNote = sampleIndexInNote,
                            noteDurationSamples = noteDurationSamples
                        ) * track.volume
                    }
                }

                val clipped = mixed / (1f + abs(mixed))
                buffer[i] = (clipped * Short.MAX_VALUE).toInt().toShort()

                globalSample++
                i++
            }
            audioTrack.write(buffer, 0, chunkSize)
        }
    }

    private fun midiToFrequency(pitch: Int): Double = 440.0 * Math.pow(2.0, (pitch - 69) / 12.0)

    private fun synthesize(
        instrument: Instrument,
        pitch: Int,
        velocity: Int,
        sampleIndexInNote: Long,
        noteDurationSamples: Long
    ): Float {
        val gain = (velocity / 127f).coerceIn(0f, 1f) * 0.25f

        if (instrument == Instrument.DRUMS) {
            return synthesizeDrum(pitch, sampleIndexInNote, noteDurationSamples) * gain
        }

        val t = sampleIndexInNote.toDouble() / SAMPLE_RATE
        val freq = midiToFrequency(pitch)
        val phase = 2.0 * PI * freq * t

        val noteLenSec = (noteDurationSamples.toDouble() / SAMPLE_RATE).coerceAtLeast(0.05)
        val attackSamples = (0.005 * SAMPLE_RATE).coerceAtLeast(1.0)
        val attack = (sampleIndexInNote.toDouble() / attackSamples).coerceIn(0.0, 1.0)
        val decay = exp(-t / (noteLenSec * 0.6 + 0.05))
        val envelope = attack * decay

        val wave = when (instrument) {
            Instrument.PIANO -> sin(phase) + 0.3 * sin(phase * 2) + 0.15 * sin(phase * 3)
            Instrument.GUITAR -> sawtooth(phase)
            Instrument.BASS -> sin(phase * 0.5)
            Instrument.SYNTH -> square(phase)
            Instrument.DRUMS -> 0.0
        }

        return (wave * envelope).toFloat() * gain
    }

    private fun sawtooth(phase: Double): Double {
        val x = (phase / (2 * PI)).let { it - Math.floor(it) }
        return 2.0 * x - 1.0
    }

    private fun square(phase: Double): Double {
        val x = (phase / (2 * PI)).let { it - Math.floor(it) }
        return if (x < 0.5) 1.0 else -1.0
    }

    private val noise = Random(0)

    private fun synthesizeDrum(pitch: Int, sampleIndexInNote: Long, noteDurationSamples: Long): Float {
        val t = sampleIndexInNote.toDouble() / SAMPLE_RATE
        val lenSec = (noteDurationSamples.toDouble() / SAMPLE_RATE).coerceAtLeast(0.05)

        return when (pitch) {
            36 -> {
                val freq = 150.0 * exp(-t * 18) + 45.0
                val env = exp(-t / (lenSec * 0.35 + 0.02))
                (sin(2 * PI * freq * t) * env).toFloat()
            }
            38 -> {
                val tone = sin(2 * PI * 180.0 * t)
                val hiss = noise.nextDouble(-1.0, 1.0)
                val env = exp(-t / (lenSec * 0.25 + 0.015))
                ((tone * 0.4 + hiss * 0.6) * env).toFloat()
            }
            42 -> {
                val hiss = noise.nextDouble(-1.0, 1.0)
                val env = exp(-t / 0.04)
                (hiss * env).toFloat()
            }
            46 -> {
                val hiss = noise.nextDouble(-1.0, 1.0)
                val env = exp(-t / (lenSec * 0.5 + 0.05))
                (hiss * env).toFloat()
            }
            45 -> {
                val freq = 200.0 * exp(-t * 10) + 90.0
                val env = exp(-t / (lenSec * 0.4 + 0.03))
                (sin(2 * PI * freq * t) * env).toFloat()
            }
            49 -> {
                val hiss = noise.nextDouble(-1.0, 1.0)
                val env = exp(-t / (lenSec * 1.2 + 0.3))
                (hiss * env).toFloat()
            }
            else -> 0f
        }
    }
}