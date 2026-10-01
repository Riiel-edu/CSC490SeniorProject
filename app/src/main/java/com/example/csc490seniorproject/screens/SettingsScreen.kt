package com.example.csc490seniorproject.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csc490seniorproject.viewmodels.LandingScreenVM

private val Purple = Color(0xFF683CF5)
private val CardPurple = Color(0xFFE0D2FF)


@Composable
fun SettingsScreen(  viewModel: LandingScreenVM,
                     modifier: Modifier = Modifier,
                     onProfileClick: () -> Unit = {}
) {
    var musicOn by rememberSaveable { mutableStateOf(true) }
    var soundEffectsOn by rememberSaveable { mutableStateOf(true) }
    var soundCheckOn by rememberSaveable { mutableStateOf(false) }
    var mobileDownloadsOn by rememberSaveable { mutableStateOf(false) }

    var audioQuality by rememberSaveable { mutableStateOf("Standard") }
    var equalizer by rememberSaveable { mutableStateOf("Off") }
    var transitions by rememberSaveable { mutableStateOf("Off") }

    var openDialog by remember { mutableStateOf<String?>(null) }
    var showAbout by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Settings ⚙",
            color = Purple,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )
        Text("Make HipPop your own", color = Color.DarkGray)

        SettingsCard("Audio") {
            SettingsSwitch(
                title = "Music",
                description = "Play music while using the app",
                checked = musicOn,
                onCheckedChange = { musicOn = it }
            )
            HorizontalDivider()

            SettingsSwitch(
                title = "Sound effects",
                description = "Play sounds when you interact with the app",
                checked = soundEffectsOn,
                onCheckedChange = { soundEffectsOn = it }
            )
            HorizontalDivider()

            SettingsChoice("Audio quality", audioQuality) {
                openDialog = "Audio quality"
            }
            HorizontalDivider()

            SettingsChoice("Equalizer", equalizer) {
                openDialog = "Equalizer"
            }
            HorizontalDivider()

            SettingsSwitch(
                title = "Sound Check",
                description = "Keep songs at a more consistent volume",
                checked = soundCheckOn,
                onCheckedChange = { soundCheckOn = it }
            )
            HorizontalDivider()

            SettingsChoice("Song transitions", transitions) {
                openDialog = "Song transitions"
            }
        }

        SettingsCard("Downloads") {
            SettingsSwitch(
                title = "Download over mobile data",
                description = "Allow downloads without Wi-Fi",
                checked = mobileDownloadsOn,
                onCheckedChange = { mobileDownloadsOn = it }
            )
        }

        SettingsCard("Account") {
            SettingsChoice(
                title = "Profile",
                value = "Your songs and account information",
                onClick = onProfileClick
            )
        }

        SettingsCard("About") {
            SettingsChoice(
                title = "HipPop",
                value = "App information",
                onClick = { showAbout = true }
            )
        }
    }

    val options = when (openDialog) {
        "Audio quality" -> listOf("Standard", "High")
        "Equalizer" -> listOf("Off", "Bass boost", "Treble boost")
        "Song transitions" -> listOf("Off", "Short fade", "Long fade")
        else -> emptyList()
    }

    if (openDialog != null) {
        AlertDialog(
            onDismissRequest = { openDialog = null },
            title = { Text(openDialog.orEmpty()) },
            text = {
                Column {
                    options.forEach { option ->
                        Text(
                            text = option,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (openDialog) {
                                        "Audio quality" -> audioQuality = option
                                        "Equalizer" -> equalizer = option
                                        "Song transitions" -> transitions = option
                                    }
                                    openDialog = null
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { openDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About HipPop") },
            text = {
                Text("Create, discover, and share music with HipPop ♫")
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = CardPurple,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = Purple,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, fontSize = 12.sp, color = Color.DarkGray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsChoice(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text("$value ›", color = Color.DarkGray)
    }
}