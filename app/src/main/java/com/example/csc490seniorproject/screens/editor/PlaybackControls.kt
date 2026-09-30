package com.example.csc490seniorproject.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csc490seniorproject.R

private val fredoka = FontFamily(Font(R.font.fredoka_medium, FontWeight.Normal))

@Composable
fun TransportControls(
    isPlaying: Boolean,
    onRestart: () -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RestartButton(onClick = onRestart)
        PlayStopButton(isPlaying = isPlaying, onPlay = onPlay, onStop = onStop)
    }
}

@Composable
fun RestartButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(Color(0xFFbfb1fa), CircleShape)
    ) {
        Icon(
            imageVector = Icons.Filled.Replay,
            contentDescription = "Restart from the beginning",
            tint = Color.White
        )
    }
}

@Composable
fun PlayStopButton(isPlaying: Boolean, onPlay: () -> Unit, onStop: () -> Unit) {
    Button(
        onClick = { if (isPlaying) onStop() else onPlay() },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF5d36eb),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Stop" else "Play"
        )
        Text(
            text = if (isPlaying) " Stop" else " Play",
            fontFamily = fredoka,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}

private val NOTE_LENGTH_OPTIONS = listOf(
    "Short" to 1,
    "Medium" to 2,
    "Long" to 4,
    "Whole" to 8
)

@Composable
fun NoteLengthSelector(selectedSteps: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        NOTE_LENGTH_OPTIONS.forEach { (label, steps) ->
            val isSelected = steps == selectedSteps
            Box(
                modifier = Modifier
                    .background(
                        if (isSelected) Color(0xFF5d36eb) else Color(0xFFbfb1fa),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = { onSelect(steps) })
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontFamily = fredoka,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun BarJumpField(totalBars: Int, onJumpToBar: (bar: Int) -> Unit) {
    var text by remember { mutableStateOf("") }

    fun submit() {
        val bar = text.toIntOrNull()
        if (bar != null) {
            onJumpToBar(bar.coerceIn(1, totalBars.coerceAtLeast(1)))
        }
        text = ""
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Go to bar", fontFamily = fredoka, fontSize = 13.sp, color = Color(0xFF5d36eb))

        OutlinedTextField(
            value = text,
            onValueChange = { new -> if (new.length <= 3 && new.all { it.isDigit() }) text = new },
            modifier = Modifier.width(70.dp),
            singleLine = true,
            placeholder = { Text("#", fontFamily = fredoka, fontSize = 13.sp) },
            textStyle = TextStyle(fontFamily = fredoka, fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { submit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF5d36eb),
                unfocusedBorderColor = Color(0xFFbfb1fa)
            )
        )

        Button(
            onClick = { submit() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5d36eb), contentColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(text = "Go", fontFamily = fredoka, fontSize = 13.sp)
        }
    }
}

@Composable
fun TempoControl(bpm: Int, onChangeBpm: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        IconButton(
            onClick = { onChangeBpm(bpm - 5) },
            modifier = Modifier.size(32.dp).background(Color(0xFFbfb1fa), CircleShape)
        ) {
            Text(text = "–", fontFamily = fredoka, color = Color.White, fontSize = 18.sp)
        }
        Text(
            text = "$bpm BPM",
            fontFamily = fredoka,
            color = Color(0xFF5d36eb),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp)
        )
        IconButton(
            onClick = { onChangeBpm(bpm + 5) },
            modifier = Modifier.size(32.dp).background(Color(0xFFbfb1fa), CircleShape)
        ) {
            Text(text = "+", fontFamily = fredoka, color = Color.White, fontSize = 18.sp)
        }
    }
}

@Composable
fun EditorToolbar(
    isPlaying: Boolean,
    bpm: Int,
    totalBars: Int,
    noteLengthSteps: Int,
    onRestart: () -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onJumpToBar: (Int) -> Unit,
    onNoteLengthSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RestartButton(onClick = onRestart)
        PlayStopButton(isPlaying = isPlaying, onPlay = onPlay, onStop = onStop)
        TempoControl(bpm = bpm, onChangeBpm = onBpmChange)
        BarJumpField(totalBars = totalBars, onJumpToBar = onJumpToBar)
        NoteLengthSelector(selectedSteps = noteLengthSteps, onSelect = onNoteLengthSelect)
    }
}