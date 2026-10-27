package com.example.csc490seniorproject.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session")

class UserSession(private val context: Context) {

    private object Keys {
        val UID = stringPreferencesKey("uid")
        val USERNAME = stringPreferencesKey("username")
        val LAST_SONG_ID = stringPreferencesKey("last_song_id")
    }

    val uid: Flow<String?> = context.sessionDataStore.data.map { it[Keys.UID] }
    val username: Flow<String?> = context.sessionDataStore.data.map { it[Keys.USERNAME] }
    val lastSongId: Flow<String?> = context.sessionDataStore.data.map { it[Keys.LAST_SONG_ID] }

    suspend fun saveSession(uid: String, username: String) {
        context.sessionDataStore.edit {
            it[Keys.UID] = uid
            it[Keys.USERNAME] = username
        }
    }

    suspend fun setLastSongId(songId: String) {
        context.sessionDataStore.edit { it[Keys.LAST_SONG_ID] = songId }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}