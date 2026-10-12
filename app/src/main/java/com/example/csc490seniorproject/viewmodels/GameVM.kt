package com.example.csc490seniorproject.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.csc490seniorproject.objects.Beatmap
import com.example.csc490seniorproject.objects.endStep
import com.example.csc490seniorproject.objects.stepDurationMs
import com.example.csc490seniorproject.objects.totalSteps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class Judgment(val points: Int, val label: String) {
    PERFECT(50, "Perfect!"),
    GOOD(30, "Good"),
    OK(10, "OK"),
    MISS(0, "Miss")
}

data class FallingNote(
    val id: String,
    val column: Int,
    val hitTimeMs: Double,
    val releaseTimeMs: Double
)
class GameVM(application: Application) : AndroidViewModel(application) {

    private var beatmap: Beatmap = Beatmap()
    var fallingNotes: List<FallingNote> = emptyList()
        private set

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()
    private val _judgedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val judgedNoteIds: StateFlow<Set<String>> = _judgedNoteIds.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _combo = MutableStateFlow(0)
    val combo: StateFlow<Int> = _combo.asStateFlow()

    private val _maxCombo = MutableStateFlow(0)
    val maxCombo: StateFlow<Int> = _maxCombo.asStateFlow()

    private val _isOnFire = MutableStateFlow(false)
    val isOnFire: StateFlow<Boolean> = _isOnFire.asStateFlow()

    // The most recent judgment, for a brief "Perfect!" / "Miss" popup. Not
    // cleared automatically — the UI decides how long to show it and clears
    // it itself (see GameScreen's LaunchedEffect), since that's a display
    // concern, not a game-logic one.
    private val _lastJudgment = MutableStateFlow<Judgment?>(null)
    val lastJudgment: StateFlow<Judgment?> = _lastJudgment.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private var job: Job? = null
    private var totalDurationMs: Double = 0.0

    /** A HOLD note currently being held down, tracked from press until release. */
    private data class ActiveHold(
        val noteId: String,
        val pressStartMs: Long,
        val basePoints: Int,
        val multiplier: Double,
        val maxHoldMs: Double
    )

    private val _activeHolds = MutableStateFlow<Map<Int, ActiveHold>>(emptyMap())

    private val _activeHoldColumns = MutableStateFlow<Set<Int>>(emptySet())
    val activeHoldColumns: StateFlow<Set<Int>> = _activeHoldColumns.asStateFlow()

    fun loadBeatmap(beatmap: Beatmap) {
        stop()
        this.beatmap = beatmap
        val stepMs = beatmap.stepDurationMs()

        fallingNotes = beatmap.notes.map { note ->
            FallingNote(
                id = note.id,
                column = note.column,
                hitTimeMs = note.startStep * stepMs + LEAD_IN_MS,
                releaseTimeMs = note.endStep() * stepMs + LEAD_IN_MS
            )
        }
        totalDurationMs = beatmap.totalSteps() * stepMs + LEAD_IN_MS
        resetRunState()
    }

    private fun resetRunState() {
        _judgedNoteIds.value = emptySet()
        _score.value = 0
        _combo.value = 0
        _maxCombo.value = 0
        _isOnFire.value = false
        _isGameOver.value = false
        _elapsedMs.value = 0
        _lastJudgment.value = null
    }

    fun start() {
        job?.cancel()
        resetRunState()
        val startTime = System.currentTimeMillis()
        job = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val now = System.currentTimeMillis() - startTime
                _elapsedMs.value = now
                autoMissExpiredNotes(now.toDouble())

                if (now > totalDurationMs + OK_WINDOW_MS + 500) {
                    _isGameOver.value = true
                    break
                }
                delay(16) // ~60fps
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    private fun autoMissExpiredNotes(nowMs: Double) {
        val judged = _judgedNoteIds.value
        val newlyMissed = fallingNotes.filter { it.id !in judged && nowMs > it.hitTimeMs + OK_WINDOW_MS }
        if (newlyMissed.isEmpty()) return
        _judgedNoteIds.value = judged + newlyMissed.map { it.id }
        registerMiss()
    }

    private fun registerMiss() {
        _combo.value = 0
        _isOnFire.value = false
        _lastJudgment.value = Judgment.MISS
    }

    fun onLanePress(column: Int) {
        if (_isGameOver.value) return
        val now = _elapsedMs.value.toDouble()
        val judged = _judgedNoteIds.value

        val candidate = fallingNotes
            .filter { it.column == column && it.id !in judged }
            .filter { abs(now - it.hitTimeMs) <= OK_WINDOW_MS }
            .minByOrNull { abs(now - it.hitTimeMs) }
            ?: return

        _judgedNoteIds.value = judged + candidate.id

        val offset = abs(now - candidate.hitTimeMs)
        val judgment = when {
            offset <= PERFECT_WINDOW_MS -> Judgment.PERFECT
            offset <= GOOD_WINDOW_MS -> Judgment.GOOD
            else -> Judgment.OK
        }
        val multiplier = registerHit(judgment)

        val isHold = candidate.releaseTimeMs > candidate.hitTimeMs
        if (isHold) {
            _activeHolds.value = _activeHolds.value + (column to ActiveHold(
                noteId = candidate.id,
                pressStartMs = _elapsedMs.value,
                basePoints = judgment.points,
                multiplier = multiplier,
                maxHoldMs = candidate.releaseTimeMs - candidate.hitTimeMs
            ))
            _activeHoldColumns.value = _activeHolds.value.keys
        }
    }

    fun onLaneRelease(column: Int) {
        val active = _activeHolds.value[column] ?: return
        _activeHolds.value = _activeHolds.value - column
        _activeHoldColumns.value = _activeHolds.value.keys

        val heldMs = (_elapsedMs.value - active.pressStartMs).coerceIn(0L, active.maxHoldMs.toLong())
        val secondsHeld = heldMs / 1000.0
        val bonus = (active.basePoints / 5.0) * secondsHeld * active.multiplier
        _score.value += bonus.toInt()
    }

    private fun registerHit(judgment: Judgment): Double {
        val newCombo = _combo.value + 1
        _combo.value = newCombo
        if (newCombo > _maxCombo.value) _maxCombo.value = newCombo

        val onFire = newCombo >= ON_FIRE_COMBO_THRESHOLD
        _isOnFire.value = onFire

        val multiplier = if (onFire) ON_FIRE_MULTIPLIER else 1.0
        _score.value += (judgment.points * multiplier).toInt()
        _lastJudgment.value = judgment
        return multiplier
    }

    companion object {
        const val FALL_DURATION_MS = 1800.0
        const val LEAD_IN_MS = FALL_DURATION_MS

        const val PERFECT_WINDOW_MS = 50.0
        const val GOOD_WINDOW_MS = 120.0
        const val OK_WINDOW_MS = 200.0

        const val ON_FIRE_COMBO_THRESHOLD = 10
        const val ON_FIRE_MULTIPLIER = 1.5
    }
}