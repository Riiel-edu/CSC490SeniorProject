package com.example.csc490seniorproject.objects

fun Song.stepsPerBar(): Int = stepsPerBeat * beatsPerBar

fun Song.totalSteps(): Int = stepsPerBar() * bars

fun Song.stepDurationMs(): Double = 60_000.0 / (bpm * stepsPerBeat)

fun Note.endStep(): Int = startStep + lengthSteps

fun Track.noteAt(pitch: Int, step: Int): Note? =
    notes.firstOrNull { it.pitch == pitch && step in it.startStep until it.endStep() }

fun newSong(ownerId: String): Song = Song(
    ownerId = ownerId,
    tracks = listOf(Track(name = "Piano", instrument = Instrument.PIANO))
)