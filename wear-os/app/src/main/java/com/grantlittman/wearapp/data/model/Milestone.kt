package com.grantlittman.wearapp.data.model

/**
 * A signal that fires at a specific elapsed time during a session.
 *
 * @param triggerAtMillis The elapsed time (in milliseconds) at which this milestone fires.
 * @param signal The signal to produce.
 * @param repeatCount How many times to fire the signal (e.g., 3 pulses for "time's up"). Default is 1.
 * @param label Optional human-readable label (e.g., "Halfway", "Wrap up", "Done").
 */
data class Milestone(
    val triggerAtMillis: Long,
    val signal: Signal,
    val repeatCount: Int = 1,
    val label: String? = null
)
