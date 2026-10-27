package com.example.csc490seniorproject.screens

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.csc490seniorproject.R
import com.example.csc490seniorproject.objects.stepsPerBar
import com.example.csc490seniorproject.screens.editor.MultiTrackEditor
import com.example.csc490seniorproject.screens.editor.TrackBar
import com.example.csc490seniorproject.viewmodels.EditorScreenVM
import com.example.csc490seniorproject.viewmodels.WorkspaceViewModel

@SuppressLint("ContextCastToActivity")
@Composable
fun EditorScreen(viewModel: EditorScreenVM, navController: NavController, modifier: Modifier = Modifier) {

    val fredoka = FontFamily(Font(R.font.fredoka_medium, FontWeight.Normal))
    val song by viewModel.song.collectAsState()
    val selectedTrackId by viewModel.selectedTrackId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val noteLengthSteps by viewModel.noteLengthSteps.collectAsState()
    val activeOctaveByTrack by viewModel.activeOctaveByTrack.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()

    val workspaceViewModel: WorkspaceViewModel = viewModel(LocalContext.current as ComponentActivity)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    workspaceViewModel.setCurrentSong(song)
                    navController.navigate("BeatmapEditorScreen")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5d36eb), contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "\uD83C\uDFAE Beatmap Editor", fontFamily = fredoka)
            }

            Button(
                onClick = { viewModel.saveCurrentSong() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2e9e5b), contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) { Text("\uD83D\uDCBE Save", fontFamily = fredoka) }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("\uD83D\uDCBF Song Editor", fontFamily = fredoka, fontSize = 22.sp, color = Color(0xFF5d36eb))

        Spacer(modifier = Modifier.height(10.dp))

        EditorToolbar(
            isPlaying = isPlaying,
            bpm = song.bpm,
            totalBars = song.bars,
            onRestart = { viewModel.restartFromStart() },
            onPlay = { viewModel.playSong() },
            onStop = { viewModel.stopPlayback() },
            onBpmChange = { bpm -> viewModel.setBpm(bpm) },
            onJumpToBar = { bar -> viewModel.seekTo((bar - 1) * song.stepsPerBar()) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        NoteLengthSelector(
            selectedSteps = noteLengthSteps,
            onSelect = { steps -> viewModel.setNoteLength(steps) }
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