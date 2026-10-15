package com.example.csc490seniorproject.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csc490seniorproject.R
import com.example.csc490seniorproject.objects.BeatNote
import com.example.csc490seniorproject.objects.BeatNoteType
import com.example.csc490seniorproject.objects.Beatmap
import com.example.csc490seniorproject.objects.endStep
import com.example.csc490seniorproject.objects.stepDurationMs
import com.example.csc490seniorproject.objects.stepsPerBar
import com.example.csc490seniorproject.objects.totalSteps

private val fredoka = FontFamily(Font(R.font.fredoka_medium, FontWeight.Normal))
val listOf4Colors = listOf(Color(0xFFFF6B6B), Color(0xFFFFC75B), Color(0xFF4DCFC0), Color(0xFF7A6BFF))
val listOf6Colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF9F6B), Color(0xFFFFE66B),
    Color(0xFF6BCB77), Color(0xFF4D96FF), Color(0xFFB26BFF)
)

fun laneColors(columnCount: Int): List<Color> = if (columnCount == 6) listOf6Colors else listOf4Colors

fun DrawScope.drawNoteCircle(center: Offset, radius: Float, color: Color) {
    drawCircle(color = Color.Black, radius = radius + 3f, center = center)
    drawCircle(color = color, radius = radius, center = center)
    drawCircle(color = Color.White, radius = radius * 0.62f, center = center)
    drawCircle(color = color, radius = radius * 0.4f, center = center)
}

@Composable
fun BeatmapEditor(
    beatmap: Beatmap,
    activeNoteType: BeatNoteType,
    onPlaceNote: (column: Int, step: Int, lengthSteps: Int) -> Unit,
    onResizeNote: (noteId: String, newLengthSteps: Int) -> Unit,
    onDeleteNote: (noteId: String) -> Unit,
    playheadStep: Int? = null,
    isPlaying: Boolean = false,
    onSeek: (step: Int) -> Unit = {}
) {
    val totalSteps = beatmap.totalSteps()
    val stepsPerBar = beatmap.stepsPerBar()
    val hScroll = rememberScrollState()
    val density = LocalDensity.current
    val cellWidthPx = with(density) { 44.dp.toPx() }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {

        val viewportWidthPx = with(density) { (maxWidth - 64.dp).toPx() }

        LaunchedEffect(playheadStep, isPlaying) {
            if (isPlaying && playheadStep != null && viewportWidthPx > 0) {
                val playheadPx = playheadStep * cellWidthPx
                val target = (playheadPx - viewportWidthPx / 2f).toInt().coerceIn(0, hScroll.maxValue)
                val stepMs = beatmap.stepDurationMs().toInt().coerceAtLeast(1)
                hScroll.animateScrollTo(target, animationSpec = tween(durationMillis = stepMs, easing = LinearEasing))
            }
        }

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.width(64.dp).height(28.dp))
                    SeekRuler(
                        totalSteps = totalSteps,
                        stepsPerBar = stepsPerBar,
                        playheadStep = playheadStep,
                        hScroll = hScroll,
                        onSeek = onSeek
                    )
                }

                Row(modifier = Modifier.fillMaxWidth().height(40.dp * beatmap.columnCount)) {
                    LaneLabels(columnCount = beatmap.columnCount)
                    BeatGrid(
                        columnCount = beatmap.columnCount,
                        notes = beatmap.notes,
                        totalSteps = totalSteps,
                        stepsPerBar = stepsPerBar,
                        hScroll = hScroll,
                        activeNoteType = activeNoteType,
                        playheadStep = playheadStep,
                        onPlaceNote = onPlaceNote,
                        onResizeNote = onResizeNote,
                        onDeleteNote = onDeleteNote
                    )
                }
            }
        }
    }
}

@Composable
private fun SeekRuler(
    totalSteps: Int,
    stepsPerBar: Int,
    playheadStep: Int?,
    hScroll: ScrollState,
    onSeek: (Int) -> Unit
) {
    val density = LocalDensity.current
    val cellWidthPx = with(density) { 44.dp.toPx() }
    val barCount = (totalSteps / stepsPerBar).coerceAtLeast(1)
    val totalWidth = 44.dp * totalSteps

    fun stepAt(x: Float): Int = (x / cellWidthPx).toInt().coerceIn(0, totalSteps - 1)

    Box(modifier = Modifier.height(28.dp).horizontalScroll(hScroll, enabled = false)) {
        Box(
            modifier = Modifier
                .width(totalWidth)
                .height(28.dp)
                .pointerInput(totalSteps) {
                    detectTapGestures { offset -> onSeek(stepAt(offset.x)) }
                }
                .pointerInput(totalSteps) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onSeek(stepAt(change.position.x))
                    }
                }
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(barCount) { barIndex ->
                    Box(
                        modifier = Modifier.width(44.dp * stepsPerBar).height(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "${barIndex + 1}", fontFamily = fredoka, fontSize = 12.sp, color = Color(0xFF5d36eb))
                    }
                }
            }
            if (playheadStep != null) {
                Box(
                    modifier = Modifier
                        .padding(start = 44.dp * playheadStep)
                        .width(3.dp)
                        .height(28.dp)
                        .background(Color(0xFFFF6B6B))
                )
            }
        }
    }
}

@Composable
private fun LaneLabels(columnCount: Int) {
    Column(modifier = Modifier.width(64.dp)) {
        for (column in 0 until columnCount) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(if (column % 2 == 0) Color.White else Color(0xFFF7F5FC)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lane ${column + 1}",
                    fontFamily = fredoka,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF444444),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BeatGrid(
    columnCount: Int,
    notes: List<BeatNote>,
    totalSteps: Int,
    stepsPerBar: Int,
    hScroll: ScrollState,
    activeNoteType: BeatNoteType,
    playheadStep: Int?,
    onPlaceNote: (column: Int, step: Int, lengthSteps: Int) -> Unit,
    onResizeNote: (noteId: String, newLengthSteps: Int) -> Unit,
    onDeleteNote: (noteId: String) -> Unit
) {
    val density = LocalDensity.current
    val cellWidthPx = with(density) { 44.dp.toPx() }
    val cellHeightPx = with(density) { 40.dp.toPx() }
    val gridWidth = 44.dp * totalSteps
    val gridHeight = 40.dp * columnCount

    Box(modifier = Modifier.horizontalScroll(hScroll, enabled = true)) {
        Canvas(
            modifier = Modifier
                .size(gridWidth, gridHeight)
                .pointerInput(notes, columnCount) {
                    detectTapGestures { offset ->
                        val step = (offset.x / cellWidthPx).toInt().coerceIn(0, totalSteps - 1)
                        val column = (offset.y / cellHeightPx).toInt().coerceIn(0, columnCount - 1)
                        val existing = notes.firstOrNull { it.column == column && step in it.startStep until it.endStep() }
                        if (existing != null) onDeleteNote(existing.id) else onPlaceNote(column, step, 1)
                    }
                }
                .pointerInput(notes, columnCount, activeNoteType) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startStep = (down.position.x / cellWidthPx).toInt().coerceIn(0, totalSteps - 1)
                        val startColumn = (down.position.y / cellHeightPx).toInt().coerceIn(0, columnCount - 1)
                        val existingAtStart = notes.firstOrNull {
                            it.column == startColumn && startStep in it.startStep until it.endStep()
                        }
                        val capturing = existingAtStart?.type == BeatNoteType.HOLD ||
                                (existingAtStart == null && activeNoteType == BeatNoteType.HOLD)

                        if (!capturing) {
                            return@awaitEachGesture
                        }

                        down.consume()
                        var liveStep = startStep
                        val pointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            change.consume()
                            if (change.changedToUpIgnoreConsumed()) break
                            liveStep = (change.position.x / cellWidthPx).toInt().coerceIn(0, totalSteps - 1)
                        }

                        val movedSteps = liveStep - startStep
                        when {

                            existingAtStart != null && existingAtStart.type == BeatNoteType.HOLD && movedSteps != 0 -> {
                                val newLength = (liveStep - existingAtStart.startStep + 1).coerceAtLeast(1)
                                onResizeNote(existingAtStart.id, newLength)
                            }
                            existingAtStart != null -> onDeleteNote(existingAtStart.id)

                            movedSteps != 0 -> {
                                val lo = minOf(startStep, liveStep)
                                val hi = maxOf(startStep, liveStep)
                                onPlaceNote(startColumn, lo, hi - lo + 1)
                            }

                            else -> onPlaceNote(startColumn, startStep, 1)
                        }
                    }
                }
        ) {

            for (column in 0 until columnCount) {
                drawRect(
                    color = if (column % 2 == 0) Color.White else Color(0xFFF7F5FC),
                    topLeft = Offset(0f, column * cellHeightPx),
                    size = Size(gridWidth.toPx(), cellHeightPx)
                )
            }

            for (column in 0..columnCount) {
                val y = column * cellHeightPx
                drawLine(Color(0xFFEDEDED), Offset(0f, y), Offset(gridWidth.toPx(), y))
            }

            for (step in 0..totalSteps) {
                val x = step * cellWidthPx
                val isBarLine = step % stepsPerBar == 0
                drawLine(
                    color = if (isBarLine) Color(0xFFbfb1fa) else Color(0xFFEDEDED),
                    start = Offset(x, 0f),
                    end = Offset(x, gridHeight.toPx()),
                    strokeWidth = if (isBarLine) 2f else 1f
                )
            }

            notes.forEach { note ->
                val color = if (note.type == BeatNoteType.HOLD) Color(0xFF7f01cd) else Color(0xFF3f47cb)
                val rowCenterY = note.column * cellHeightPx + cellHeightPx / 2f
                val headX = note.startStep * cellWidthPx + cellWidthPx / 2f
                val radius = minOf(cellWidthPx, cellHeightPx) * 0.38f

                if (note.type == BeatNoteType.HOLD) {
                    val tailX = note.endStep() * cellWidthPx - cellWidthPx / 2f
                    val barHeight = radius * 1.5f
                    val barWidth = (tailX - headX).coerceAtLeast(0f)

                    drawRect(
                        color = color,
                        topLeft = Offset(headX, rowCenterY - barHeight / 2f),
                        size = Size(barWidth, barHeight)
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(headX, rowCenterY - barHeight * 0.12f),
                        size = Size(barWidth, barHeight * 0.24f)
                    )

                    drawNoteCircle(center = Offset(headX, rowCenterY), radius = radius, color = color)
                    drawNoteCircle(center = Offset(tailX, rowCenterY), radius = radius, color = color)
                } else {
                    drawNoteCircle(center = Offset(headX, rowCenterY), radius = radius, color = color)
                }
            }

            // The play bar — drawn last so it sits on top of the notes.
            if (playheadStep != null) {
                val x = playheadStep * cellWidthPx
                drawLine(color = Color(0xFF33313F), start = Offset(x, 0f), end = Offset(x, gridHeight.toPx()), strokeWidth = 3f)
            }
        }
    }
}