package com.grantlittman.wearapp.data.model

/**
 * The type of haptic vibration to produce.
 */
enum class HapticType {
    /** Short, subtle vibration (~50ms). Discreet. */
    TAP,

    /** Medium vibration (~200ms). Noticeable but subtle. */
    BUZZ,

    /** Long, strong vibration (~500ms). Unmistakable. */
    PULSE,

    /** Two quick taps in succession. Easy to distinguish from a single tap. */
    DOUBLE_TAP
}

/**
 * The type of audio tone to play (optional, off by default).
 */
enum class AudioType {
    /** Short, quiet tone. */
    CLICK,

    /** Standard alert tone. */
    BEEP,

    /** Longer, more noticeable tone. */
    CHIME
}

/**
 * A single haptic/audio event the watch can produce.
 *
 * @param hapticType The vibration style.
 * @param intensity Vibration strength from 0.0 (off) to 1.0 (max). Default is 1.0.
 * @param audioType Optional audio tone to accompany the haptic. Null means no audio.
 */
data class Signal(
    val hapticType: HapticType = HapticType.TAP,
    val intensity: Float = 1.0f,
    val audioType: AudioType? = null
)
