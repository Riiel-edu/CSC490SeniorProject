package com.example.csc490seniorproject.objects

fun Beatmap.stepsPerBar(): Int = stepsPerBeat * beatsPerBar
fun Beatmap.totalSteps(): Int = stepsPerBar() * bars
fun Beatmap.stepDurationMs(): Double = 60_000.0 / (bpm * stepsPerBeat)
fun BeatNote.endStep(): Int = startStep + lengthSteps

fun validColumnCount(count: Int): Int = if (count == 6) 6 else 4

fun newBeatmap(songId: String, ownerId: String, columnCount: Int = 4, bpm: Int = 120, bars: Int = 4): Beatmap =
    Beatmap(
        songId = songId,
        ownerId = ownerId,
        columnCount = validColumnCount(columnCount),
        bpm = bpm,
        bars = bars
    )