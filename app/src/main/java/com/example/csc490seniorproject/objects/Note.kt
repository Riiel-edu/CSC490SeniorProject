package com.example.csc490seniorproject.objects

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val pitch: Int = 60,
    val startStep: Int = 0,
    val lengthSteps: Int = 1,
    val velocity: Int = 100
)
