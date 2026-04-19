package com.grantlittman.wearapp.timer

import com.grantlittman.wearapp.data.model.Milestone

/**
 * Observable state of the running timer, exposed to the UI.
 */
data class TimerState(
    /** Whether the timer is currently running (not paused). */
    val isRunning: Boolean = false,

    /** Whether the timer is paused. */
    val isPaused: Boolean = false,

    /** Total elapsed time in milliseconds (pauses excluded). */
    val elapsedMillis: Long = 0L,

    /** Total duration of the pattern in milliseconds, or null if indefinite. */
    val totalDurationMillis: Long? = null,

    /** The name of the active pattern. */
    val patternName: String = "",

    /** The next upcoming milestone, if any. */
    val nextMilestone: Milestone? = null,

    /** Whether the countdown warning phase is active. */
    val inCountdown: Boolean = false,

    /**
     * Monotonic timestamp (System.currentTimeMillis) of the last signal fired.
     * UI uses this to trigger a brief visual flash. Each new value triggers the animation.
     */
    val lastSignalFiredAt: Long = 0L,

    /** True if the last signal was a milestone (vs. a regular interval or countdown). */
    val lastSignalWasMilestone: Boolean = false
) {
    /** Time remaining in milliseconds, or null if indefinite. */
    val remainingMillis: Long?
        get() = totalDurationMillis?.let { (it - elapsedMillis).coerceAtLeast(0L) }

    /** Whether the session has reached its total duration. */
    val isFinished: Boolean
        get() = totalDurationMillis != null && elapsedMillis >= totalDurationMillis
}
