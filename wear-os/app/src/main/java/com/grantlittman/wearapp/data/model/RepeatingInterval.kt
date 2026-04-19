package com.grantlittman.wearapp.data.model

/**
 * The core heartbeat of a pattern — a signal that fires at a fixed recurring interval.
 *
 * @param intervalMillis Time between each signal firing, in milliseconds.
 * @param signal The signal to produce at each interval.
 */
data class RepeatingInterval(
    val intervalMillis: Long,
    val signal: Signal
)
