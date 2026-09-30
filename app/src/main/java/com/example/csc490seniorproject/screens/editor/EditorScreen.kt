package com.example.csc490seniorproject.screens.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.csc490seniorproject.objects.stepsPerBar
import com.example.csc490seniorproject.viewmodels.EditorScreenVM

@Composable
fun EditorScreen(viewModel: EditorScreenVM, modifier: Modifier = Modifier) {
    val song by viewModel.song.collectAsState()
    val selectedTrackId by viewModel.selectedTrackId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val noteLengthSteps by viewModel.noteLengthSteps.collectAsState()
    val activeOctaveByTrack by viewModel.activeOctaveByTrack.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        EditorToolbar(
            isPlaying = isPlaying,
            bpm = song.bpm,
            totalBars = song.bars,
            noteLengthSteps = noteLengthSteps,
            onRestart = { viewModel.restartFromStart() },
            onPlay = { viewModel.playSong() },
            onStop = { viewModel.stopPlayback() },
            onBpmChange = { bpm -> viewModel.setBpm(bpm) },
            onJumpToBar = { bar -> viewModel.seekTo((bar - 1) * song.stepsPerBar()) },
            onNoteLengthSelect = { steps -> viewModel.setNoteLength(steps) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TrackBar(
            song = song,
            selectedTrackId = selectedTrackId,
            onSelectTrack = { viewModel.selectTrack(it) },
            onAddTrack = { name, instrument -> viewModel.addTrack(name, instrument) },
            onDeleteTrack = { viewModel.deleteTrack(it) },
            onToggleMute = { viewModel.toggleMute(it) },
            onToggleSolo = { viewModel.toggleSolo(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        MultiTrackEditor(
            song = song,
            currentStep = currentStep,
            isPlaying = isPlaying,
            activeOctaveByTrack = activeOctaveByTrack,
            onSeek = { step -> viewModel.seekTo(step) },
            onOctaveChange = { trackId, octave -> viewModel.setActiveOctave(trackId, octave) },
            onPlaceNote = { trackId, pitch, step -> viewModel.placeNote(trackId, pitch, step) },
            onDeleteNote = { trackId, noteId -> viewModel.deleteNote(trackId, noteId) }
        )
    }
}