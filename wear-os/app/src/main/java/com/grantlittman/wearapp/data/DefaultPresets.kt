package com.grantlittman.wearapp.data

import com.grantlittman.wearapp.data.model.CountdownWarning
import com.grantlittman.wearapp.data.model.HapticType
import com.grantlittman.wearapp.data.model.Milestone
import com.grantlittman.wearapp.data.model.Pattern
import com.grantlittman.wearapp.data.model.RepeatingInterval
import com.grantlittman.wearapp.data.model.Signal

/**
 * Built-in presets that ship with the app. These are seeded on first launch
 * and cannot be deleted by the user.
 */
object DefaultPresets {

    private const val SECOND = 1_000L
    private const val MINUTE = 60 * SECOND

    val all: List<Pattern> = listOf(
        lightningTalk(),
        presentationThirtyMin(),
        simpleMetronome()
    )

    /**
     * 5-Minute Lightning Talk
     * - Tap every 60 seconds
     * - Double tap at 4:00 ("1 minute left")
     * - Pulse at 5:00 ("Done")
     */
    private fun lightningTalk() = Pattern(
        id = "preset_lightning_talk",
        name = "5-Min Lightning Talk",
        isPreset = true,
        totalDurationMillis = 5 * MINUTE,
        repeatingInterval = RepeatingInterval(
            intervalMillis = 60 * SECOND,
            signal = Signal(hapticType = HapticType.TAP)
        ),
        milestones = listOf(
            Milestone(
                triggerAtMillis = 4 * MINUTE,
                signal = Signal(hapticType = HapticType.DOUBLE_TAP),
                label = "1 minute left"
            ),
            Milestone(
                triggerAtMillis = 5 * MINUTE,
                signal = Signal(hapticType = HapticType.PULSE),
                repeatCount = 2,
                label = "Done"
            )
        )
    )

    /**
     * 30-Minute Presentation
     * - Tap every 5 minutes
     * - Double tap at 15:00 ("Halfway")
     * - Buzz every 30 seconds during last 2 minutes
     * - Pulse at 25:00 ("5 min left"), 28:00 ("Wrap up"), 30:00 ("Done")
     */
    private fun presentationThirtyMin() = Pattern(
        id = "preset_30min_presentation",
        name = "30-Min Presentation",
        isPreset = true,
        totalDurationMillis = 30 * MINUTE,
        repeatingInterval = RepeatingInterval(
            intervalMillis = 5 * MINUTE,
            signal = Signal(hapticType = HapticType.TAP)
        ),
        milestones = listOf(
            Milestone(
                triggerAtMillis = 15 * MINUTE,
                signal = Signal(hapticType = HapticType.DOUBLE_TAP),
                label = "Halfway"
            ),
            Milestone(
                triggerAtMillis = 25 * MINUTE,
                signal = Signal(hapticType = HapticType.PULSE),
                label = "5 min left"
            ),
            Milestone(
                triggerAtMillis = 28 * MINUTE,
                signal = Signal(hapticType = HapticType.PULSE),
                label = "Wrap up"
            ),
            Milestone(
                triggerAtMillis = 30 * MINUTE,
                signal = Signal(hapticType = HapticType.PULSE),
                repeatCount = 3,
                label = "Done"
            )
        ),
        countdownWarning = CountdownWarning(
            activateAtMillis = 2 * MINUTE,
            intervalMillis = 30 * SECOND,
            signal = Signal(hapticType = HapticType.BUZZ)
        )
    )

    /**
     * Simple Metronome
     * - Tap every 10 seconds
     * - No milestones, no end time
     * - Runs until manually stopped
     */
    private fun simpleMetronome() = Pattern(
        id = "preset_simple_metronome",
        name = "Simple Metronome",
        isPreset = true,
        totalDurationMillis = null,
        repeatingInterval = RepeatingInterval(
            intervalMillis = 10 * SECOND,
            signal = Signal(hapticType = HapticType.TAP)
        )
    )
}
