package com.example.csc490seniorproject.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.csc490seniorproject.data.SongRepository
import com.example.csc490seniorproject.objects.Beatmap
import com.example.csc490seniorproject.objects.Song
import com.example.csc490seniorproject.objects.newSong
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MySongsVM : ViewModel() {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refresh() = viewModelScope.launch {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
        try {
            _songs.value = SongRepository.mySongs(uid).sortedByDescending { it.updatedAt?.time ?: 0L }
        } catch (e: Exception) {
            _error.value = "Couldn't load songs: ${e.message}"
        }
    }

    /** Creates the song AND its beatmap up front, then hands back the id to open in the editor. */
    fun createSong(name: String, genre: List<String>, onCreated: (String) -> Unit) = viewModelScope.launch {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { _error.value = "Not signed in"; return@launch }
        try {
            val song = newSong(ownerId = uid).copy(name = name.ifBlank { "Untitled Song" }, genre = genre)
            val map = Beatmap(
                songId = song.id, ownerId = uid, title = song.name, bpm = song.bpm,
                stepsPerBeat = song.stepsPerBeat, beatsPerBar = song.beatsPerBar, bars = song.bars
            )
            SongRepository.saveAll(song, map)
            onCreated(song.id)
        } catch (e: Exception) {
            _error.value = "Couldn't create song: ${e.message}"
        }
    }

    fun updateDetails(id: String, name: String, genre: List<String>) = viewModelScope.launch {
        try { SongRepository.updateSongDetails(id, name, genre); refresh() }
        catch (e: Exception) { _error.value = "Couldn't update: ${e.message}" }
    }

    fun togglePublish(song: Song) = viewModelScope.launch {
        try { SongRepository.setPublished(song.id, !song.published); refresh() }
        catch (e: Exception) { _error.value = "Couldn't publish: ${e.message}" }
    }

    fun delete(id: String) = viewModelScope.launch {
        try { SongRepository.deleteSong(id); refresh() }
        catch (e: Exception) { _error.value = "Couldn't delete: ${e.message}" }
    }
}