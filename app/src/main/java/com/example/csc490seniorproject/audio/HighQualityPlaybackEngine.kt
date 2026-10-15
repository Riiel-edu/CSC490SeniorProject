package com.example.csc490seniorproject.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.csc490seniorproject.objects.Instrument
import com.example.csc490seniorproject.objects.Song
import com.example.csc490seniorproject.objects.Track
import com.example.csc490seniorproject.objects.stepDurationMs
import com.example.csc490seniorproject.objects.totalSteps
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

data class SampleZone(val resId: Int, val basePitch: Int)

class HighQualityPlaybackEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(24)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()

    private val resIdToSoundId = mutableMapOf<Int, Int>()
    private val readySignals = mutableMapOf<Int, CompletableDeferred<Unit>>()

    init {
        soundPool.setOnLoadCompleteListener { _, soundId, _ ->
            readySignals[soundId]?.complete(Unit)
        }
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private var job: Job? = null

    @Volatile private var seekRequestStep: Int? = null

    private fun ensureLoaded(resId: Int): Int {
        resIdToSoundId[resId]?.let { return it }
        val soundId = soundPool.load(context, resId, 1)
        resIdToSoundId[resId] = soundId
        readySignals[soundId] = CompletableDeferred()
        return soundId
    }

    suspend fun preload(song: Song) {
        val resIds = mutableSetOf<Int>()
        song.tracks.forEach { track ->
            if (track.instrument == Instrument.DRUMS) {
                resIds += drumSampleMap().values
            } else {
                resIds += sampleZonesForInstrument(track.instrument).map { it.resId }
            }
        }
        val soundIds = resIds.map { ensureLoaded(it) }
        soundIds.forEach { soundId -> readySignals[soundId]?.await() }
    }

    fun previewNote(instrument: Instrument, pitch: Int, velocity: Int = 100) {
        scope.launch(Dispatchers.Default) {
            playOneShotSuspending(instrument, pitch, velocity, volume = 1f)
        }
    }

    fun play(song: Song, fromStep: Int? = null, startDelayMs: Long = 0L) {
        stop()
        if (song.tracks.isEmpty() || song.totalSteps() <= 0) return
        if (fromStep != null) {
            _currentStep.value = fromStep.coerceIn(0, song.totalSteps() - 1)
        }
        seekRequestStep = null // drop any stale seek left over from a previous run
        _isPlaying.value = true
        job = scope.launch(Dispatchers.Default) {
            preload(song) // fast no-op after the first time; guarantees sounds are ready before the loop fires them
            if (startDelayMs > 0) delay(startDelayMs)
            scheduleLoop(song)
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
        if (_isPlaying.value) seekRequestStep = clamped
    }

    fun release() {
        stop()
        soundPool.release()
    }

    private suspend fun scheduleLoop(song: Song) {
        val stepMs = song.stepDurationMs()
        val totalSteps = song.totalSteps()
        val anySolo = song.tracks.any { it.solo }
        fun isAudible(t: Track) = if (anySolo) t.solo else !t.muted

        var step = _currentStep.value
        var referenceTime = System.currentTimeMillis() - (step * stepMs).toLong()

        while (scope.isActive && _isPlaying.value) {
            seekRequestStep?.let { seekStep ->
                step = seekStep
                referenceTime = System.currentTimeMillis() - (step * stepMs).toLong()
                seekRequestStep = null
            }

            _currentStep.value = step

            for (track in song.tracks) {
                if (!isAudible(track)) continue
                for (note in track.notes) {
                    if (note.startStep == step) {
                        playOneShot(
                            instrument = track.instrument,
                            pitch = note.pitch,
                            velocity = note.velocity,
                            volume = track.volume,
                            durationMs = note.lengthSteps * stepMs
                        )
                    }
                }
            }

            val nextStepTime = referenceTime + ((step + 1) * stepMs).toLong()
            val delayMs = nextStepTime - System.currentTimeMillis()
            if (delayMs > 0) delay(delayMs)

            step = (step + 1) % totalSteps
            if (step == 0) {
                // Looped back to the start — re-anchor so tiny scheduling drift
                // over a long loop never has a chance to accumulate.
                referenceTime = System.currentTimeMillis()
            }
        }
    }

    private fun playOneShot(instrument: Instrument, pitch: Int, velocity: Int, volume: Float, durationMs: Double) {
        val vol = (velocity / 127f).coerceIn(0f, 1f) * volume.coerceIn(0f, 1f) * VOICE_GAIN

        if (instrument == Instrument.DRUMS) {
            val resId = drumSampleMap()[pitch] ?: return
            val soundId = resIdToSoundId[resId] ?: return
            soundPool.play(soundId, vol, vol, 1, 0, 1f)
            return
        }

        val zone = sampleZonesForInstrument(instrument).minByOrNull { abs(it.basePitch - pitch) } ?: return
        val soundId = resIdToSoundId[zone.resId] ?: return
        val rate = 2.0.pow((pitch - zone.basePitch) / 12.0).toFloat().coerceIn(0.5f, 2.0f)

        if (instrument !in SUSTAINED_INSTRUMENTS) {
            soundPool.play(soundId, vol, vol, 1, 0, rate) // one-shot, rings out naturally
            return
        }

        val streamId = soundPool.play(soundId, vol, vol, 1, -1, rate) // loop = -1 (infinite)
        if (streamId != 0) {
            scope.launch(Dispatchers.Default) {
                val totalMs = durationMs.toLong().coerceAtLeast(1)
                val fadeMs = minOf(FADE_OUT_MS, totalMs)
                delay(totalMs - fadeMs)
                val steps = (fadeMs / FADE_STEP_MS).toInt().coerceAtLeast(1)
                for (i in 1..steps) {
                    val v = vol * (1f - i / steps.toFloat())
                    soundPool.setVolume(streamId, v, v)
                    delay(FADE_STEP_MS)
                }
                soundPool.stop(streamId)
            }
        }
    }

    private suspend fun playOneShotSuspending(instrument: Instrument, pitch: Int, velocity: Int, volume: Float) {
        val vol = (velocity / 127f).coerceIn(0f, 1f) * volume.coerceIn(0f, 1f) * VOICE_GAIN

        val resId: Int
        val rate: Float
        if (instrument == Instrument.DRUMS) {
            resId = drumSampleMap()[pitch] ?: return
            rate = 1f
        } else {
            val zone = sampleZonesForInstrument(instrument).minByOrNull { abs(it.basePitch - pitch) } ?: return
            resId = zone.resId
            rate = 2.0.pow((pitch - zone.basePitch) / 12.0).toFloat().coerceIn(0.5f, 2.0f)
        }

        val soundId = ensureLoaded(resId)
        readySignals[soundId]?.let { if (!it.isCompleted) it.await() }
        soundPool.play(soundId, vol, vol, 1, 0, rate)
    }

    private companion object {
        const val VOICE_GAIN = 0.6f

        val SUSTAINED_INSTRUMENTS = setOf(Instrument.SYNTH)

        const val FADE_OUT_MS = 40L
        const val FADE_STEP_MS = 5L
    }
}
