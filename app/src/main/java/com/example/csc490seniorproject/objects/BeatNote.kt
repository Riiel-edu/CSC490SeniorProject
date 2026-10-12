package com.example.csc490seniorproject.objects

import java.util.UUID

data class BeatNote(
    val id: String = UUID.randomUUID().toString(),
    val column: Int = 0,
    val startStep: Int = 0,
    val lengthSteps: Int = 1,
    val type: BeatNoteType = BeatNoteType.TAP
)