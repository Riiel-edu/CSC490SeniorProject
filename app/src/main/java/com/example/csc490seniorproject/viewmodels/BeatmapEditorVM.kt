package com.example.csc490seniorproject.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.csc490seniorproject.objects.BeatNote
import com.example.csc490seniorproject.objects.BeatNoteType
import com.example.csc490seniorproject.objects.Beatmap
import com.example.csc490seniorproject.objects.newBeatmap
import com.example.csc490seniorproject.objects.validColumnCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BeatmapEditorVM(application: Application) : AndroidViewModel(application) {

    private val beatmapFlow = MutableStateFlow(newBeatmap(songId = "", ownerId = ""))
    val beatmap: StateFlow<Beatmap> = beatmapFlow.asStateFlow()

    private val undoStack = ArrayDeque<Beatmap>()
    private val maxUndo = 50
    val canUndo: Boolean get() = undoStack.isNotEmpty()

    private val activeNoteTypeFlow = MutableStateFlow(BeatNoteType.TAP)
    val activeNoteType: StateFlow<BeatNoteType> = activeNoteTypeFlow.asStateFlow()

    fun setActiveNoteType(type: BeatNoteType) {
        activeNoteTypeFlow.value = type
    }

    fun loadBeatmap(beatmap: Beatmap) {
        undoStack.clear()
        beatmapFlow.value = beatmap
    }

    fun startNewBeatmap(songId: String, ownerId: String, columnCount: Int = 4, bpm: Int = 120, bars: Int = 4) {
        undoStack.clear()
        beatmapFlow.value = newBeatmap(songId, ownerId, columnCount, bpm, bars)
    }

    fun renameBeatmap(title: String) = mutate { it.copy(title = title) }

    fun setColumnCount(count: Int) = mutate { it.copy(columnCount = validColumnCount(count)) }

    fun setBpm(bpm: Int) = mutate { it.copy(bpm = bpm.coerceIn(20, 300)) }

    fun setBars(bars: Int) = mutate { it.copy(bars = bars.coerceIn(1, 64)) }

    fun placeNote(column: Int, step: Int, lengthSteps: Int = 1) = mutate { beatmap ->
        val type = activeNoteTypeFlow.value
        val length = if (type == BeatNoteType.HOLD) lengthSteps.coerceAtLeast(1) else 1
        val withoutOverlap = beatmap.notes.filterNot { it.column == column && it.startStep == step }
        beatmap.copy(notes = withoutOverlap + BeatNote(column = column, startStep = step, lengthSteps = length, type = type))
    }

    fun resizeNote(noteId: String, newLengthSteps: Int) = mutate { beatmap ->
        val notes = beatmap.notes.map { note ->
            if (note.id == noteId && note.type == BeatNoteType.HOLD) {
                note.copy(lengthSteps = newLengthSteps.coerceAtLeast(1))
            } else {
                note
            }
        }
        beatmap.copy(notes = notes)
    }

    fun deleteNote(noteId: String) = mutate { beatmap ->
        beatmap.copy(notes = beatmap.notes.filterNot { it.id == noteId })
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        beatmapFlow.value = previous
    }

    private inline fun mutate(transform: (Beatmap) -> Beatmap) {
        val current = beatmapFlow.value
        val updated = transform(current)
        if (updated == current) return
        undoStack.addLast(current)
        if (undoStack.size > maxUndo) undoStack.removeFirst()
        beatmapFlow.value = updated
    }
}