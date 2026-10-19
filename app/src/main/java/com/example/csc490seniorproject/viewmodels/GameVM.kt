
package com.example.csc490seniorproject.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.csc490seniorproject.objects.Beatmap
import com.example.csc490seniorproject.objects.BeatNoteType
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

enum class StarRating(val label: String, val starsFilled: Int) {
    ZERO("", 0),
    ONE("1 Star", 1),
    TWO("2 Stars", 2),
    THREE("3 Stars", 3),
    FOUR("4 Stars", 4),
    FIVE("5 Stars", 5),
    GOLD("\u2728 GOLD! \u2728", 5)
}

fun starsForPercent(percent: Double): StarRating = when {
    percent >= 100.0 -> StarRating.GOLD
    percent >= 87.5 -> StarRating.FIVE
    percent >= 70.0 -> StarRating.FOUR
    percent >= 52.5 -> StarRating.THREE
    percent >= 35.0 -> StarRating.TWO
    percent >= 17.5 -> StarRating.ONE
    else -> StarRating.ZERO
}

data class FallingNote(
    val id: String,
    val column: Int,
    val hitTimeMs: Double,
    val releaseTimeMs: Double,
    val type: BeatNoteType
)

data class TapHitEffect(
    val id: String,
    val column: Int
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

    private val _lastJudgment = MutableStateFlow<Judgment?>(null)
    val lastJudgment: StateFlow<Judgment?> = _lastJudgment.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _perfectCount = MutableStateFlow(0)
    val perfectCount: StateFlow<Int> = _perfectCount.asStateFlow()

    private val _goodCount = MutableStateFlow(0)
    val goodCount: StateFlow<Int> = _goodCount.asStateFlow()

    private val _okCount = MutableStateFlow(0)
    val okCount: StateFlow<Int> = _okCount.asStateFlow()

    private val _missCount = MutableStateFlow(0)
    val missCount: StateFlow<Int> = _missCount.asStateFlow()

    private val _tapHitEffect = MutableStateFlow<TapHitEffect?>(null)
    val tapHitEffect: StateFlow<TapHitEffect?> = _tapHitEffect.asStateFlow()

    private val _holdReleaseEffect = MutableStateFlow<TapHitEffect?>(null)

    val holdReleaseEffect: StateFlow<TapHitEffect?> =
        _holdReleaseEffect.asStateFlow()

    var maxPossibleScore: Int = 0
        private set

    private var job: Job? = null
    private var totalDurationMs: Double = 0.0

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
                releaseTimeMs = note.endStep() * stepMs + LEAD_IN_MS,
                type = note.type
            )
        }

        totalDurationMs = beatmap.totalSteps() * stepMs + LEAD_IN_MS
        maxPossibleScore = computeMaxScore(beatmap, stepMs)
        resetRunState()
    }

    private fun computeMaxScore(beatmap: Beatmap, stepMs: Double): Int {
        var currentCombo = 0
        var total = 0.0

        beatmap.notes.sortedBy { it.startStep }.forEach { note ->
            currentCombo++

            val multiplier =
                if (currentCombo >= ON_FIRE_COMBO_THRESHOLD) {
                    ON_FIRE_MULTIPLIER
                } else {
                    1.0
                }

            total += Judgment.PERFECT.points * multiplier

            if (note.type == BeatNoteType.HOLD) {
                val fullHoldSeconds = (note.lengthSteps * stepMs) / 1000.0
                total += (Judgment.PERFECT.points / 5.0) *
                        fullHoldSeconds * multiplier
            }
        }

        return total.toInt()
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
        _perfectCount.value = 0
        _goodCount.value = 0
        _okCount.value = 0
        _missCount.value = 0
        _activeHolds.value = emptyMap()
        _activeHoldColumns.value = emptySet()
        _tapHitEffect.value = null
        _holdReleaseEffect.value = null
    }

    fun start() {
        job?.cancel()
        resetRunState()

        val startTime = System.currentTimeMillis()

        job = viewModelScope.launch(Dispatchers.Default) {
            var completionStartedAt: Long? = null

            while (isActive) {
                val now = System.currentTimeMillis() - startTime
                _elapsedMs.value = now

                autoMissExpiredNotes(now.toDouble())

                val allJudged =
                    _judgedNoteIds.value.size >= fallingNotes.size

                val noActiveHolds = _activeHolds.value.isEmpty()

                // All notes must be judged and every hold must be released.
                val gameplayComplete = allJudged && noActiveHolds

                if (gameplayComplete) {
                    if (completionStartedAt == null) {
                        completionStartedAt = now
                    }

                    if (now - completionStartedAt >= END_DELAY_MS) {
                        _isGameOver.value = true
                        break
                    }
                } else {
                    // Do not count down toward game over while a hold remains.
                    completionStartedAt = null
                }

                delay(16)
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

        val newlyMissed = fallingNotes.filter { note ->
            note.id !in judged &&
                    nowMs > note.hitTimeMs + OK_WINDOW_MS
        }

        if (newlyMissed.isEmpty()) return

        _judgedNoteIds.value = judged + newlyMissed.map { it.id }
        _missCount.value = _missCount.value + newlyMissed.size

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
        val isHold = candidate.type == BeatNoteType.HOLD

        if (isHold) {
            _activeHolds.value = _activeHolds.value + (
                    column to ActiveHold(
                        noteId = candidate.id,
                        pressStartMs = _elapsedMs.value,
                        basePoints = judgment.points,
                        multiplier = multiplier,
                        maxHoldMs = candidate.releaseTimeMs - candidate.hitTimeMs
                    )
                    )

            _activeHoldColumns.value = _activeHolds.value.keys
        } else {
            // A unique note ID triggers a short TAP pop animation.
            _tapHitEffect.value = TapHitEffect(
                id = candidate.id,
                column = column
            )
        }
    }

    fun onLaneRelease(column: Int) {
        val active = _activeHolds.value[column] ?: return

        // Trigger a pop effect at the lane where the hold was released.
        _holdReleaseEffect.value = TapHitEffect(
            id = active.noteId,
            column = column
        )

        _activeHolds.value = _activeHolds.value - column
        _activeHoldColumns.value = _activeHolds.value.keys

        val heldMs = (
                _elapsedMs.value - active.pressStartMs
                ).coerceIn(0L, active.maxHoldMs.toLong())

        val secondsHeld = heldMs / 1000.0
        val bonus = (active.basePoints / 5.0) *
                secondsHeld * active.multiplier

        _score.value += bonus.toInt()
    }

    private fun registerHit(judgment: Judgment): Double {
        when (judgment) {
            Judgment.PERFECT -> _perfectCount.value++
            Judgment.GOOD -> _goodCount.value++
            Judgment.OK -> _okCount.value++
            Judgment.MISS -> Unit
        }

        val newCombo = _combo.value + 1
        _combo.value = newCombo

        if (newCombo > _maxCombo.value) {
            _maxCombo.value = newCombo
        }

        val onFire = newCombo >= ON_FIRE_COMBO_THRESHOLD
        _isOnFire.value = onFire

        val multiplier =
            if (onFire) ON_FIRE_MULTIPLIER else 1.0

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

        const val END_DELAY_MS = 1_000L
    }
}
