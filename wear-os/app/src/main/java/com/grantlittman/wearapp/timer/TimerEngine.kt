package com.grantlittman.wearapp.timer

import com.grantlittman.wearapp.data.model.HapticType
import com.grantlittman.wearapp.data.model.Milestone
import com.grantlittman.wearapp.data.model.Pattern
import com.grantlittman.wearapp.data.model.Signal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The core timer logic. Tracks elapsed time and decides when to fire
 * repeating intervals, milestones, and countdown warnings.
 *
 * This class is lifecycle-agnostic — it's driven by a CoroutineScope
 * provided by the foreground service.
 */
class TimerEngine(
    private val signalExecutor: SignalExecutor,
    private val scope: CoroutineScope
) {

    companion object {
        /** Fast tick for interactive UI — smooth progress display. */
        private const val TICK_FAST_MS = 100L
        /** Slow tick for ambient/background — saves battery while still firing signals on time. */
        private const val TICK_SLOW_MS = 500L
    }

    /** Set by the activity to slow ticks when the screen is in ambient mode. */
    var isAmbient: Boolean = false

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var pattern: Pattern? = null

    // Tracking what has already fired to avoid duplicates
    private var lastIntervalFiredAt: Long = 0L
    private val firedMilestoneIds = mutableSetOf<Long>() // triggerAtMillis as key
    private var lastCountdownFiredAt: Long = 0L

    /**
     * Start the timer with a given pattern.
     */
    fun start(pattern: Pattern) {
        this.pattern = pattern
        lastIntervalFiredAt = 0L
        firedMilestoneIds.clear()
        lastCountdownFiredAt = 0L

        _state.value = TimerState(
            isRunning = true,
            isPaused = false,
            elapsedMillis = 0L,
            totalDurationMillis = pattern.totalDurationMillis,
            patternName = pattern.name,
            nextMilestone = findNextMilestone(0L, pattern.milestones)
        )

        startTicking()
    }

    /**
     * Pause the timer — signals stop, elapsed time freezes.
     */
    fun pause() {
        tickJob?.cancel()
        tickJob = null
        _state.value = _state.value.copy(isRunning = false, isPaused = true)
    }

    /**
     * Resume from a paused state.
     */
    fun resume() {
        _state.value = _state.value.copy(isRunning = true, isPaused = false)
        startTicking()
    }

    /**
     * Stop the timer completely. State is preserved (not reset) so the UI
     * can still display the final values during the navigation transition.
     * The next call to start() will initialize fresh state.
     */
    fun stop() {
        tickJob?.cancel()
        tickJob = null
        signalExecutor.cancel()
        pattern = null
        _state.value = _state.value.copy(isRunning = false, isPaused = false)
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            var lastTickTime = System.currentTimeMillis()

            while (isActive) {
                delay(if (isAmbient) TICK_SLOW_MS else TICK_FAST_MS)

                val now = System.currentTimeMillis()
                val delta = now - lastTickTime
                lastTickTime = now

                val currentState = _state.value
                val newElapsed = currentState.elapsedMillis + delta
                val p = pattern ?: return@launch

                // Check if we've finished
                if (p.totalDurationMillis != null && newElapsed >= p.totalDurationMillis) {
                    // Fire any final milestones before stopping
                    checkMilestones(newElapsed, p)

                    // Fire a strong completion signal so the user knows time is up
                    _state.value = currentState.copy(
                        elapsedMillis = p.totalDurationMillis,
                        isRunning = false,
                        nextMilestone = null,
                        lastSignalFiredAt = System.currentTimeMillis(),
                        lastSignalWasMilestone = true
                    )
                    scope.launch {
                        signalExecutor.execute(
                            signal = Signal(
                                hapticType = HapticType.PULSE,
                                intensity = 1.0f
                            ),
                            audioEnabled = p.audioEnabled,
                            repeatCount = 3
                        )
                    }
                    return@launch
                }

                // Update elapsed time
                _state.value = currentState.copy(
                    elapsedMillis = newElapsed,
                    nextMilestone = findNextMilestone(newElapsed, p.milestones),
                    inCountdown = isInCountdown(newElapsed, p)
                )

                // Check and fire signals — milestones take priority over intervals
                val milestoneFired = checkMilestones(newElapsed, p)
                if (!milestoneFired) {
                    checkRepeatingInterval(newElapsed, p)
                }
                checkCountdownWarning(newElapsed, p)
            }
        }
    }

    /**
     * Fire the repeating interval signal if enough time has passed.
     */
    private fun checkRepeatingInterval(elapsed: Long, pattern: Pattern) {
        val interval = pattern.repeatingInterval.intervalMillis
        if (interval <= 0) return

        // How many intervals should have fired by now?
        val expectedCount = elapsed / interval
        val firedCount = lastIntervalFiredAt / interval

        if (expectedCount > firedCount) {
            lastIntervalFiredAt = expectedCount * interval
            _state.value = _state.value.copy(
                lastSignalFiredAt = System.currentTimeMillis(),
                lastSignalWasMilestone = false
            )
            scope.launch {
                signalExecutor.execute(
                    signal = pattern.repeatingInterval.signal,
                    audioEnabled = pattern.audioEnabled
                )
            }
        }
    }

    /**
     * Fire any milestones whose trigger time has been reached.
     * Returns true if at least one milestone fired (so the interval can be suppressed).
     */
    private fun checkMilestones(elapsed: Long, pattern: Pattern): Boolean {
        var fired = false
        for (milestone in pattern.milestones) {
            if (milestone.triggerAtMillis in 1..elapsed &&
                milestone.triggerAtMillis !in firedMilestoneIds
            ) {
                firedMilestoneIds.add(milestone.triggerAtMillis)
                fired = true
                _state.value = _state.value.copy(
                    lastSignalFiredAt = System.currentTimeMillis(),
                    lastSignalWasMilestone = true
                )
                scope.launch {
                    signalExecutor.execute(
                        signal = milestone.signal,
                        audioEnabled = pattern.audioEnabled,
                        repeatCount = milestone.repeatCount
                    )
                }
            }
        }
        return fired
    }

    /**
     * Fire countdown warning signals at an increased frequency during the final stretch.
     */
    private fun checkCountdownWarning(elapsed: Long, pattern: Pattern) {
        val warning = pattern.countdownWarning ?: return
        val totalDuration = pattern.totalDurationMillis ?: return

        val countdownStart = (totalDuration - warning.activateAtMillis).coerceAtLeast(0L)
        if (elapsed < countdownStart) return

        val elapsedInCountdown = elapsed - countdownStart
        val interval = warning.intervalMillis
        if (interval <= 0) return

        val lastElapsedInCountdown = (lastCountdownFiredAt - countdownStart).coerceAtLeast(0L)
        val expectedCount = elapsedInCountdown / interval
        val firedCount = lastElapsedInCountdown / interval

        if (expectedCount > firedCount) {
            lastCountdownFiredAt = countdownStart + (expectedCount * interval)
            _state.value = _state.value.copy(
                lastSignalFiredAt = System.currentTimeMillis(),
                lastSignalWasMilestone = false
            )
            scope.launch {
                signalExecutor.execute(
                    signal = warning.signal,
                    audioEnabled = pattern.audioEnabled
                )
            }
        }
    }

    private fun isInCountdown(elapsed: Long, pattern: Pattern): Boolean {
        val warning = pattern.countdownWarning ?: return false
        val totalDuration = pattern.totalDurationMillis ?: return false
        return elapsed >= (totalDuration - warning.activateAtMillis)
    }

    private fun findNextMilestone(elapsed: Long, milestones: List<Milestone>): Milestone? {
        return milestones
            .filter { it.triggerAtMillis > elapsed }
            .minByOrNull { it.triggerAtMillis }
    }
}
