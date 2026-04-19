package com.grantlittman.wearapp.data.model

import java.util.UUID

/**
 * A named, saveable timer configuration that defines the complete timing
 * behavior for a session.
 */
data class Pattern(
    /** Unique identifier. */
    val id: String = UUID.randomUUID().toString(),

    /** User-visible name (e.g., "My TED Talk"). */
    val name: String,

    /** Whether this is a built-in preset (non-deletable). */
    val isPreset: Boolean = false,

    /** Total session length in milliseconds. Null means the timer runs until manually stopped. */
    val totalDurationMillis: Long? = null,

    /** The core repeating interval — a signal that fires at a fixed period. */
    val repeatingInterval: RepeatingInterval,

    /** Signals that fire at specific elapsed times (e.g., halfway, wrap-up, done). */
    val milestones: List<Milestone> = emptyList(),

    /** Optional countdown behavior for the final stretch of a session. */
    val countdownWarning: CountdownWarning? = null,

    /** Whether audio signals are enabled alongside haptic. Default is false (discreet mode). */
    val audioEnabled: Boolean = false,

    /** Timestamp of when this pattern was last used, for sorting. Null if never used. */
    val lastUsedAt: Long? = null
)
