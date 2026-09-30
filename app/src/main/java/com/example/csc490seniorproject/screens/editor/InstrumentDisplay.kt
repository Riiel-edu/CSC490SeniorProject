package com.example.csc490seniorproject.screens.editor

import com.example.csc490seniorproject.objects.Instrument

fun Instrument.emoji(): String = when (this) {
    Instrument.PIANO -> "🎹"
    Instrument.GUITAR -> "🎸"
    Instrument.BASS -> "🎻"
    Instrument.SYNTH -> "🎛️"
    Instrument.DRUMS -> "🥁"
}

fun Instrument.displayName(): String = when (this) {
    Instrument.PIANO -> "Piano"
    Instrument.GUITAR -> "Guitar"
    Instrument.BASS -> "Bass"
    Instrument.SYNTH -> "Synth"
    Instrument.DRUMS -> "Drums"
}

fun defaultPitchFor(instrument: Instrument): Int = if (instrument == Instrument.DRUMS) 36 else 60