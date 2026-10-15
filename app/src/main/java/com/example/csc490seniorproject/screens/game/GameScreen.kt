package com.example.csc490seniorproject.screens

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.csc490seniorproject.R
import com.example.csc490seniorproject.objects.Beatmap
import com.example.csc490seniorproject.viewmodels.GameVM
import com.example.csc490seniorproject.viewmodels.FallingNote
import com.example.csc490seniorproject.viewmodels.Judgment
import com.example.csc490seniorproject.viewmodels.WorkspaceViewModel
import kotlinx.coroutines.delay

private val Fredoka = FontFamily(Font(R.font.fredoka_medium, FontWeight.Normal))
private val GameBackground = Color(0xFF221B3F)
private val BUTTON_ROW_HEIGHT = 90.dp

private val FALL_DURATION_MS = GameVM.FALL_DURATION_MS

@SuppressLint("ContextCastToActivity")
@Composable
fun GameScreen(viewModel: GameVM, beatmap: Beatmap, modifier: Modifier = Modifier) {
    val workspaceViewModel: WorkspaceViewModel = viewModel(LocalContext.current as ComponentActivity)

    LaunchedEffect(beatmap.id) {
        viewModel.loadBeatmap(beatmap)
        workspaceViewModel.prepareSong()
        viewModel.start()
        workspaceViewModel.playSongFromStart(startDelayMs = GameVM.LEAD_IN_MS.toLong())
    }

    DisposableEffect(Unit) {
        onDispose { workspaceViewModel.stopSong() }
    }

    val elapsedMs by viewModel.elapsedMs.collectAsState()
    val judgedIds by viewModel.judgedNoteIds.collectAsState()
    val score by viewModel.score.collectAsState()
    val combo by viewModel.combo.collectAsState()
    val isOnFire by viewModel.isOnFire.collectAsState()
    val lastJudgment by viewModel.lastJudgment.collectAsState()
    val isGameOver by viewModel.isGameOver.collectAsState()
    val maxCombo by viewModel.maxCombo.collectAsState()
    val activeHoldColumns by viewModel.activeHoldColumns.collectAsState()

    LaunchedEffect(isGameOver) {
        if (isGameOver) workspaceViewModel.stopSong()
    }

    val colors = laneColors(beatmap.columnCount)

    val visibleNotes = remember(elapsedMs, judgedIds) {
        viewModel.fallingNotes.filter { note ->
            note.id !in judgedIds &&
                    note.hitTimeMs - FALL_DURATION_MS <= elapsedMs &&
                    note.hitTimeMs >= elapsedMs - 200
        }
    }

    Box(modifier = modifier.fillMaxSize().background(GameBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScoreHud(score = score, combo = combo, isOnFire = isOnFire)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                HitButtonsRow(
                    columnCount = beatmap.columnCount,
                    colors = colors,
                    activeHoldColumns = activeHoldColumns,
                    onLanePress = { column -> viewModel.onLanePress(column) },
                    onLaneRelease = { column -> viewModel.onLaneRelease(column) },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(BUTTON_ROW_HEIGHT)
                )

                FallingNotesField(
                    columnCount = beatmap.columnCount,
                    notes = visibleNotes,
                    elapsedMs = elapsedMs.toDouble()
                )
                JudgmentPopup(judgment = lastJudgment)
            }
        }

        if (isGameOver) {
            GameOverOverlay(score = score, maxCombo = maxCombo)
        }
    }
}

@Composable
private fun ScoreHud(score: Int, combo: Int, isOnFire: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$score", fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)

        if (combo > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isOnFire) {
                    Text(text = "\uD83D\uDD25 ", fontSize = 20.sp)
                }
                Text(
                    text = "$combo combo",
                    fontFamily = Fredoka,
                    fontSize = 16.sp,
                    color = if (isOnFire) Color(0xFFFF9F6B) else Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun JudgmentPopup(judgment: Judgment?) {
    var visible by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(judgment) {
        if (judgment != null) {
            visible = true
            delay(500)
            visible = false
        }
    }

    AnimatedVisibility(visible = visible && judgment != null, enter = fadeIn(), exit = fadeOut()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = judgment?.label ?: "",
                fontFamily = Fredoka,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                color = when (judgment) {
                    Judgment.PERFECT -> Color(0xFFF4E04D)
                    Judgment.GOOD -> Color(0xFF6BCB77)
                    Judgment.OK -> Color(0xFF4D96FF)
                    Judgment.MISS -> Color(0xFFFF6B6B)
                    null -> Color.White
                }
            )
        }
    }
}

@Composable
private fun FallingNotesField(columnCount: Int, notes: List<FallingNote>, elapsedMs: Double) {
    val density = LocalDensity.current
    val buttonHeightPx = with(density) { BUTTON_ROW_HEIGHT.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val laneWidth = size.width / columnCount
        val hitLineY = size.height - buttonHeightPx / 2f

        for (column in 0..columnCount) {
            val x = column * laneWidth
            drawLine(Color.White.copy(alpha = 0.08f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
        }

        notes.forEach { note ->
            val isHold = note.releaseTimeMs > note.hitTimeMs
            val color = if (isHold) Color.Green else Color.Blue
            val laneX = note.column * laneWidth + laneWidth / 2f
            val radius = laneWidth * 0.3f

            val progress = ((elapsedMs - (note.hitTimeMs - FALL_DURATION_MS)) / FALL_DURATION_MS).coerceIn(0.0, 1.3)
            val headY = (hitLineY * progress).toFloat()

            if (isHold) {
                val tailProgress = ((elapsedMs - (note.releaseTimeMs - FALL_DURATION_MS)) / FALL_DURATION_MS).coerceIn(0.0, 1.3)
                val tailY = (hitLineY * tailProgress).toFloat()
                val barWidth = radius * 1.5f
                val barHeight = (headY - tailY).coerceAtLeast(0f)

                drawRect(
                    color = color,
                    topLeft = Offset(laneX - barWidth / 2f, tailY),
                    size = Size(barWidth, barHeight)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(laneX - barWidth * 0.12f, tailY),
                    size = Size(barWidth * 0.24f, barHeight)
                )

                drawNoteCircle(center = Offset(laneX, tailY), radius = radius, color = color)
                drawNoteCircle(center = Offset(laneX, headY), radius = radius, color = color)
            } else {
                drawNoteCircle(center = Offset(laneX, headY), radius = radius, color = color)
            }
        }
    }
}

@Composable
private fun HitButtonsRow(
    columnCount: Int,
    colors: List<Color>,
    activeHoldColumns: Set<Int>,
    onLanePress: (Int) -> Unit,
    onLaneRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (column in 0 until columnCount) {
            val isHeld = column in activeHoldColumns
            Surface(
                color = if (isHeld) colors[column] else colors[column].copy(alpha = 0.75f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .pointerInput(column) {
                        detectTapGestures(
                            onPress = {
                                onLanePress(column)
                                tryAwaitRelease()
                                onLaneRelease(column)
                            }
                        )
                    }
            ) {}
        }
    }
}

@Composable
private fun GameOverOverlay(score: Int, maxCombo: Int) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Song Complete!", fontFamily = Fredoka, fontSize = 24.sp, color = Color.White)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Score: $score", fontFamily = Fredoka, fontSize = 20.sp, color = Color(0xFFF4E04D))
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Best Combo: $maxCombo", fontFamily = Fredoka, fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}