package com.example.csc490seniorproject.ui.theme

import android.widget.Toast
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun SaveFab(
    status: String?,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val saving = status == "Saving…"

    // Toast on result. The status passes through "Saving…" between saves, so this re-fires each time.
    LaunchedEffect(status) {
        if (status == "Saved" || status?.startsWith("Save failed") == true) {
            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
        }
    }

    ExtendedFloatingActionButton(
        onClick = { if (!saving) onSave() },
        containerColor = Color(0xFF5d36eb),
        contentColor = Color.White,
        modifier = modifier
    ) {
        Text(if (saving) "Saving…" else "Save")
    }
}