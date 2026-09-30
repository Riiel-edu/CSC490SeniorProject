package com.example.csc490seniorproject.objects

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.UUID

data class Song(
    @DocumentId val id: String = UUID.randomUUID().toString(),
    val ownerId: String = "",
    val name: String = "Untitled Song",
    val rating: Double = 0.0,
    val bpm: Int = 120,
    val stepsPerBeat: Int = 4,
    val beatsPerBar: Int = 4,
    val bars: Int = 4,
    val tracks: List<Track> = emptyList(),
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)
