package com.example.csc490seniorproject.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.csc490seniorproject.audio.HighQualityPlaybackEngine
import com.example.csc490seniorproject.data.SongRepository
import com.example.csc490seniorproject.objects.Beatmap
import com.example.csc490seniorproject.objects.Instrument
import com.example.csc490seniorproject.objects.Note
import com.example.csc490seniorproject.objects.Song
import com.example.csc490seniorproject.objects.Track
import com.example.csc490seniorproject.objects.newSong
import com.example.csc490seniorproject.objects.totalSteps
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the song currently being edited. Every mutating function here follows the
 * same pattern: build a NEW Song (copy), push the old one onto the undo stack,
 * then publish the new one. Nothing here touches Firebase or audio — those are
 * separate layers that just read this state.
 */
class EditorScreenVM(application: Application) : AndroidViewModel(application) {

    // Starts with no owner set. Call setOwnerId(...) once, right after creation,
    // with the signed-in user's id (e.g. FirebaseAuth.getInstance().currentUser?.uid).
    private val _song = MutableStateFlow(newSong(ownerId = ""))
    val song: StateFlow<Song> = _song.asStateFlow()

    // Which track is currently selected/active for editing in the piano roll.
    private val _selectedTrackId = MutableStateFlow(_song.value.tracks.first().id)
    val selectedTrackId: StateFlow<String> = _selectedTrackId.asStateFlow()

    // --- Note length ----------------------------------------------------------
    // The length (in steps) that the NEXT tapped-in note will use. Doesn't
    // affect notes already placed — pick a length, then tap to place notes
    // that long, the same way you'd pick a pencil size before drawing.
    private val _noteLengthSteps = MutableStateFlow(1)
    val noteLengthSteps: StateFlow<Int> = _noteLengthSteps.asStateFlow()

    fun setNoteLength(steps: Int) {
        _noteLengthSteps.value = steps.coerceIn(1, 16)
    }

    // --- Active octave per track -------------------------------------------------
    // Which octave a NEWLY placed note uses for each (melodic) track — the grid
    // itself shows one row per note name (no octave repeats); this decides which
    // actual octave tapping a row resolves to. Editor UI state, never saved.
    private val _activeOctaveByTrack = MutableStateFlow<Map<String, Int>>(emptyMap())
    val activeOctaveByTrack: StateFlow<Map<String, Int>> = _activeOctaveByTrack.asStateFlow()

    fun setActiveOctave(trackId: String, octave: Int) {
        _activeOctaveByTrack.value = _activeOctaveByTrack.value + (trackId to octave.coerceIn(2, 7))
    }

    /** Plays a pitch once without placing a note. */
    fun previewPitch(instrument: Instrument, pitch: Int) = playbackEngine.previewNote(instrument, pitch)

    // --- Playback -----------------------------------------------------------------
    // One engine per ViewModel instance. viewModelScope is cancelled automatically
    // when this ViewModel is cleared, which stops any in-progress schedule loop too.
    // SamplePlaybackEngine needs a Context to load sample files, which AndroidViewModel
    // gives us via getApplication() — no manual wiring needed at the call site.
    private val playbackEngine = HighQualityPlaybackEngine(getApplication(), viewModelScope)
    val isPlaying: StateFlow<Boolean> = playbackEngine.isPlaying
    val currentStep: StateFlow<Int> = playbackEngine.currentStep

    init {
        // Warms up SoundPool for the starting track's instrument so the very
        // first tap-to-place preview doesn't have to wait on a cold sample load.
        viewModelScope.launch { playbackEngine.preload(_song.value) }
    }

    /** Plays the current song from wherever the playhead currently is, looping until stopPlayback() is called. */
    fun playSong() = playbackEngine.play(_song.value)

    fun stopPlayback() = playbackEngine.stop()

    /**
     * Moves the playhead to [step]. Works whether the song is currently playing
     * (jumps the live audio immediately) or stopped (just sets where the NEXT
     * playSong() call will start from) — both are handled inside the engine.
     */
    fun seekTo(step: Int) = playbackEngine.seekTo(step, _song.value.totalSteps())

    /** Convenience for a dedicated "restart" button: jumps the playhead straight back to the beginning. */
    fun restartFromStart() = seekTo(0)

    override fun onCleared() {
        playbackEngine.release()
        super.onCleared()
    }

    // Undo support. Redo is intentionally left out for now — most mobile note
    // editors skip it, and it's easy to add later by keeping a second stack.
    private val undoStack = ArrayDeque<Song>()
    private val maxUndo = 50

    val canUndo: Boolean get() = undoStack.isNotEmpty()

    // --- Loading an existing song (used later by the Firebase layer) ---------

    /** Replaces the in-memory song wholesale, e.g. after loading from Firestore. Clears undo. */
    fun loadSong(song: Song) {
        undoStack.clear()
        _song.value = song
        _selectedTrackId.value = song.tracks.firstOrNull()?.id ?: ""
    }

    /**
     * Sets which user owns this song. Call this once, right after the ViewModel is
     * created, with the signed-in user's id. Bypasses undo/mutate on purpose —
     * this is bookkeeping, not something the user should be able to "undo".
     */
    fun setOwnerId(ownerId: String) {
        _song.value = _song.value.copy(ownerId = ownerId)
    }

    fun renameSong(name: String) = mutate { it.copy(name = name) }

    fun setBpm(bpm: Int) = mutate { it.copy(bpm = bpm.coerceIn(20, 300)) }

    fun setBars(bars: Int) = mutate { it.copy(bars = bars.coerceIn(1, 64)) }

    // --- Track management -----------------------------------------------------

    fun addTrack(name: String, instrument: Instrument) {
        mutate { song ->
            val track = Track(name = name, instrument = instrument)
            _selectedTrackId.value = track.id
            song.copy(tracks = song.tracks + track)
        }
        // Warm up this instrument's samples right away, so the first note
        // placed on the new track previews instantly instead of waiting on
        // a cold load the first time it's tapped.
        viewModelScope.launch { playbackEngine.preload(_song.value) }
    }

    fun deleteTrack(trackId: String) = mutate { song ->
        val remaining = song.tracks.filterNot { it.id == trackId }
        if (_selectedTrackId.value == trackId) {
            _selectedTrackId.value = remaining.firstOrNull()?.id ?: ""
        }
        song.copy(tracks = remaining)
    }

    fun selectTrack(trackId: String) {
        _selectedTrackId.value = trackId
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

    // --- Note editing -----------------------------------------------------------
    // Every function here takes an explicit trackId now, rather than relying on
    // "the selected track" — now that every instrument is visible and editable
    // at once (stacked, not tabbed), each grid edits its own track directly.

    /**
     * Places a note on the given track, replacing any note that already occupies
     * that exact pitch/step (tapping an occupied cell overwrites it, which is the
     * behavior people expect from grid-style note editors). Defaults to whatever
     * length is currently selected via setNoteLength(), and plays the note once
     * immediately so placing it doubles as hearing it.
     */
    fun placeNote(trackId: String, pitch: Int, startStep: Int, lengthSteps: Int = _noteLengthSteps.value, velocity: Int = 100) {
        mutate { song ->
            song.withTrack(trackId) { track ->
                val withoutOverlap = track.notes.filterNot { it.pitch == pitch && it.startStep == startStep }
                track.copy(notes = withoutOverlap + Note(pitch = pitch, startStep = startStep, lengthSteps = lengthSteps, velocity = velocity))
            }
        }
        val instrument = _song.value.tracks.firstOrNull { it.id == trackId }?.instrument ?: return
        playbackEngine.previewNote(instrument, pitch, velocity)
    }

    fun deleteNote(trackId: String, noteId: String) = mutate { song ->
        song.withTrack(trackId) { track ->
            track.copy(notes = track.notes.filterNot { it.id == noteId })
        }
    }

    /** Drags an existing note to a new pitch/step, keeping its id, length, and velocity. */
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

    // --- Undo -----------------------------------------------------------------

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        _song.value = previous
    }

    // --- Internal helpers -------------------------------------------------------

    /** Runs [transform] on the current song, pushing the old value onto the undo stack first. */
    private inline fun mutate(transform: (Song) -> Song) {
        val current = _song.value
        val updated = transform(current)
        if (updated == current) return // no-op edit, don't pollute undo history
        undoStack.addLast(current)
        if (undoStack.size > maxUndo) undoStack.removeFirst()
        _song.value = updated
    }

    private fun Song.withTrack(trackId: String, transform: (Track) -> Track): Song =
        copy(tracks = tracks.map { if (it.id == trackId) transform(it) else it })

    private val _saveStatus = MutableStateFlow<String?>(null)
    val saveStatus = _saveStatus.asStateFlow()


    fun openSong(songId: String) {
        if (_song.value.id == songId) return   // already loaded, keep unsaved edits

        viewModelScope.launch {
            try {
                val s = SongRepository.loadSong(songId)
                if (s == null) { _saveStatus.value = "Song not found"; return@launch }
                loadSong(s)
                playbackEngine.preload(s)
            } catch (e: Exception) {
                Log.e("SongSave", "load failed", e)
                _saveStatus.value = "Load failed: ${e.message}"
            }
        }
    }

    fun saveCurrentSong() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { _saveStatus.value = "Not signed in"; return }

        viewModelScope.launch {
            _saveStatus.value = "Saving…"
            try {
                val s = _song.value.copy(ownerId = uid)
                SongRepository.saveSong(s)

                val saved = SongRepository.loadSong(s.id)
                _song.update { it.copy(ownerId = uid, createdAt = saved?.createdAt, updatedAt = saved?.updatedAt) }
                _saveStatus.value = "Saved"
            } catch (e: Exception) {
                Log.e("SongSave", "failed", e)
                _saveStatus.value = "Save failed: ${e.message}"
            }
        }
    }
}