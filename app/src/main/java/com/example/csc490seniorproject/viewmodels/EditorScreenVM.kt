package com.example.csc490seniorproject.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.csc490seniorproject.audio.PlaybackEngine
import com.example.csc490seniorproject.objects.Instrument
import com.example.csc490seniorproject.objects.Note
import com.example.csc490seniorproject.objects.Song
import com.example.csc490seniorproject.objects.Track
import com.example.csc490seniorproject.objects.newSong
import com.example.csc490seniorproject.objects.totalSteps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditorScreenVM(application: Application) : AndroidViewModel(application) {

    private val songFlow = MutableStateFlow(newSong(ownerId = ""))
    val song: StateFlow<Song> = songFlow.asStateFlow()

    private val selectedTrackIdFlow = MutableStateFlow(songFlow.value.tracks.first().id)
    val selectedTrackId: StateFlow<String> = selectedTrackIdFlow.asStateFlow()

    private val noteLengthFlow = MutableStateFlow(1)
    val noteLengthSteps: StateFlow<Int> = noteLengthFlow.asStateFlow()

    fun setNoteLength(steps: Int) {
        noteLengthFlow.value = steps.coerceIn(1, 16)
    }

    private val activeOctaveByTrackFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
    val activeOctaveByTrack: StateFlow<Map<String, Int>> = activeOctaveByTrackFlow.asStateFlow()

    fun setActiveOctave(trackId: String, octave: Int) {
        activeOctaveByTrackFlow.value = activeOctaveByTrackFlow.value + (trackId to octave.coerceIn(2, 7))
    }

    fun previewPitch(instrument: Instrument, pitch: Int) = playbackEngine.previewNote(instrument, pitch)

    private val playbackEngine = PlaybackEngine(viewModelScope)
    val isPlaying: StateFlow<Boolean> = playbackEngine.isPlaying
    val currentStep: StateFlow<Int> = playbackEngine.currentStep

    fun playSong() = playbackEngine.play(songFlow.value)

    fun stopPlayback() = playbackEngine.stop()

    fun seekTo(step: Int) = playbackEngine.seekTo(step, songFlow.value.totalSteps())

    fun restartFromStart() = seekTo(0)

    override fun onCleared() {
        playbackEngine.stop()
        super.onCleared()
    }

    private val undoStack = ArrayDeque<Song>()
    private val maxUndo = 50

    val canUndo: Boolean get() = undoStack.isNotEmpty()

    fun loadSong(song: Song) {
        undoStack.clear()
        songFlow.value = song
        selectedTrackIdFlow.value = song.tracks.firstOrNull()?.id ?: ""
    }

    fun setOwnerId(ownerId: String) {
        songFlow.value = songFlow.value.copy(ownerId = ownerId)
    }

    fun renameSong(name: String) = mutate { it.copy(name = name) }

    fun setBpm(bpm: Int) = mutate { it.copy(bpm = bpm.coerceIn(20, 300)) }

    fun setBars(bars: Int) = mutate { it.copy(bars = bars.coerceIn(1, 64)) }


    fun addTrack(name: String, instrument: Instrument) = mutate { song ->
        val track = Track(name = name, instrument = instrument)
        selectedTrackIdFlow.value = track.id
        song.copy(tracks = song.tracks + track)
    }

    fun deleteTrack(trackId: String) = mutate { song ->
        val remaining = song.tracks.filterNot { it.id == trackId }
        if (selectedTrackIdFlow.value == trackId) {
            selectedTrackIdFlow.value = remaining.firstOrNull()?.id ?: ""
        }
        song.copy(tracks = remaining)
    }

    fun selectTrack(trackId: String) {
        selectedTrackIdFlow.value = trackId
    }

    fun setTrackVolume(trackId: String, volume: Float) = mutate { song ->
        song.withTrack(trackId) { it.copy(volume = volume.coerceIn(0f, 1f)) }
    }

    fun toggleMute(trackId: String) = mutate { song ->
        song.withTrack(trackId) { it.copy(muted = !it.muted) }
    }

    fun toggleSolo(trackId: String) = mutate { song ->
        song.withTrack(trackId) { it.copy(solo = !it.solo) }
    }

    fun placeNote(trackId: String, pitch: Int, startStep: Int, lengthSteps: Int = noteLengthFlow.value, velocity: Int = 100) {
        mutate { song ->
            song.withTrack(trackId) { track ->
                val withoutOverlap = track.notes.filterNot { it.pitch == pitch && it.startStep == startStep }
                track.copy(notes = withoutOverlap + Note(pitch = pitch, startStep = startStep, lengthSteps = lengthSteps, velocity = velocity))
            }
        }
        val instrument = songFlow.value.tracks.firstOrNull { it.id == trackId }?.instrument ?: return
        playbackEngine.previewNote(instrument, pitch, velocity)
    }

    fun deleteNote(trackId: String, noteId: String) = mutate { song ->
        song.withTrack(trackId) { track ->
            track.copy(notes = track.notes.filterNot { it.id == noteId })
        }
    }

    fun moveNote(trackId: String, noteId: String, newPitch: Int, newStartStep: Int) = mutate { song ->
        song.withTrack(trackId) { track ->
            val notes = track.notes.map { note ->
                if (note.id == noteId) note.copy(pitch = newPitch, startStep = newStartStep) else note
            }
            track.copy(notes = notes)
        }
    }

    fun resizeNote(trackId: String, noteId: String, newLengthSteps: Int) = mutate { song ->
        song.withTrack(trackId) { track ->
            val notes = track.notes.map { note ->
                if (note.id == noteId) note.copy(lengthSteps = newLengthSteps.coerceAtLeast(1)) else note
            }
            track.copy(notes = notes)
        }
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        songFlow.value = previous
    }

    private inline fun mutate(transform: (Song) -> Song) {
        val current = songFlow.value
        val updated = transform(current)
        if (updated == current) return // no-op edit, don't pollute undo history
        undoStack.addLast(current)
        if (undoStack.size > maxUndo) undoStack.removeFirst()
        songFlow.value = updated
    }

    private fun Song.withTrack(trackId: String, transform: (Track) -> Track): Song =
        copy(tracks = tracks.map { if (it.id == trackId) transform(it) else it })
}