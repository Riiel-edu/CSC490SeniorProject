package com.example.csc490seniorproject.audio

import com.example.csc490seniorproject.R
import com.example.csc490seniorproject.objects.Instrument

fun sampleZonesForInstrument(instrument: Instrument): List<SampleZone> = when (instrument) {
    Instrument.PIANO -> listOf(
        SampleZone(resId = R.raw.piano_c4, basePitch = 60)
    )
    Instrument.GUITAR -> listOf(
        SampleZone(resId = R.raw.guitar_c4, basePitch = 60)
    )
    Instrument.BASS -> listOf(
        SampleZone(resId = R.raw.bass_c3, basePitch = 48)
    )
    Instrument.SYNTH -> listOf(
        SampleZone(resId = R.raw.synth_c4, basePitch = 60)
    )
    Instrument.DRUMS -> emptyList()
}

fun drumSampleMap(): Map<Int, Int> = mapOf(
    36 to R.raw.drum_kick,
    38 to R.raw.drum_snare,
    42 to R.raw.drum_hat_closed,
    45 to R.raw.drum_tom,
    46 to R.raw.drum_hat_open,
    49 to R.raw.drum_crash
)