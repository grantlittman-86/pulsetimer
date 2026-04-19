package com.grantlittman.wearapp.data.model

/**
 * A distinct signal pattern that activates during the final stretch of a session.
 *
 * @param activateAtMillis How many milliseconds before the end to start the countdown
 *                         (e.g., 120_000 = activate with 2 minutes remaining).
 * @param intervalMillis How often to fire during the countdown (e.g., 15_000 = every 15 seconds).
 * @param signal The signal to produce during the countdown.
 */
data class CountdownWarning(
    val activateAtMillis: Long,
    val intervalMillis: Long,
    val signal: Signal
)
