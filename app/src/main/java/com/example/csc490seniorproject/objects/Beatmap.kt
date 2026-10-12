package com.example.csc490seniorproject.objects

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Beatmap(
    @DocumentId val id: String = "",
    val songId: String = "",
    val ownerId: String = "",
    val title: String = "Untitled Beatmap",
    val columnCount: Int = 4,
    val bpm: Int = 120,
    val stepsPerBeat: Int = 4,
    val beatsPerBar: Int = 4,
    val bars: Int = 4,
    val notes: List<BeatNote> = emptyList(),
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)