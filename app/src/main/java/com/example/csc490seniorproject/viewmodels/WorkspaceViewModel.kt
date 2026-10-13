package com.example.csc490seniorproject.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.csc490seniorproject.objects.Beatmap
import com.example.csc490seniorproject.objects.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds "whatever the user is currently working on" so the Song editor, the
 * Beatmap editor, and the Game screen can pass data to each other without
 * Firebase. Each of those screens' own ViewModels (EditorScreenVM,
 * BeatmapEditorVM, BeatmapGameVM) are scoped to their own route as usual —
 * this one is scoped to the ACTIVITY instead (see the composable examples
 * below), so it survives navigating between routes and lets one screen set a
 * Song/Beatmap that the next screen then reads.
 *
 * This is explicitly a temporary bridge. Once Firestore save/load is wired
 * up, navigation would pass a song/beatmap ID instead, and each screen would
 * load its own copy from the database — this class goes away at that point.
 */
class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _currentBeatmap = MutableStateFlow<Beatmap?>(null)
    val currentBeatmap: StateFlow<Beatmap?> = _currentBeatmap.asStateFlow()

    fun setCurrentSong(song: Song) {
        _currentSong.value = song
    }

    fun setCurrentBeatmap(beatmap: Beatmap) {
        _currentBeatmap.value = beatmap
    }
}