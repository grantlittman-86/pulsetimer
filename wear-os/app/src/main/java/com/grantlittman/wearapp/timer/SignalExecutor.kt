package com.grantlittman.wearapp.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.grantlittman.wearapp.data.model.AudioType
import com.grantlittman.wearapp.data.model.HapticType
import com.grantlittman.wearapp.data.model.Signal
import kotlinx.coroutines.delay

/**
 * Executes Signal objects by producing real haptic vibrations and optional audio tones.
 *
 * Maps each HapticType to a specific vibration pattern and each AudioType
 * to a ToneGenerator tone.
 */
class SignalExecutor(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Fire a signal — vibrate and optionally play audio.
     *
     * @param signal The signal definition.
     * @param audioEnabled Whether audio should play (controlled at the pattern level).
     * @param repeatCount How many times to fire this signal (for milestones like "3 pulses").
     */
    suspend fun execute(signal: Signal, audioEnabled: Boolean = false, repeatCount: Int = 1) {
        repeat(repeatCount) { i ->
            // Cancel any in-progress vibration so this one is clean
            vibrator.cancel()
            delay(30L) // brief settle time for the vibrator hardware
            vibrate(signal.hapticType, signal.intensity)
            if (audioEnabled) {
                // Use signal's specific audioType, or default to BEEP
                val tone = signal.audioType ?: AudioType.BEEP
                playTone(tone)
            }
            // Pause between repeats (but not after the last one)
            if (i < repeatCount - 1) {
                delay(getRepeatGapMillis(signal.hapticType))
            }
        }
    }

    private fun vibrate(type: HapticType, intensity: Float) {
        val amplitude = (intensity.coerceIn(0f, 1f) * 255).toInt().coerceAtLeast(1)

        when (type) {
            HapticType.TAP -> {
                val effect = VibrationEffect.createOneShot(50L, amplitude)
                vibrator.vibrate(effect)
            }

            HapticType.BUZZ -> {
                val effect = VibrationEffect.createOneShot(200L, amplitude)
                vibrator.vibrate(effect)
            }

            HapticType.PULSE -> {
                val effect = VibrationEffect.createOneShot(500L, amplitude)
                vibrator.vibrate(effect)
            }

            HapticType.DOUBLE_TAP -> {
                // Two quick taps: 50ms on, 100ms off, 50ms on
                val timings = longArrayOf(0, 50, 100, 50)
                val amplitudes = intArrayOf(0, amplitude, 0, amplitude)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            }
        }
    }

    /**
     * Returns the gap (in ms) to wait between repeated fires of the same signal,
     * so they don't blur together.
     */
    private fun getRepeatGapMillis(type: HapticType): Long = when (type) {
        HapticType.TAP -> 300L
        HapticType.BUZZ -> 400L
        HapticType.PULSE -> 700L
        HapticType.DOUBLE_TAP -> 500L
    }

    private fun playTone(audioType: AudioType) {
        val toneType = when (audioType) {
            AudioType.CLICK -> ToneGenerator.TONE_PROP_ACK
            AudioType.BEEP -> ToneGenerator.TONE_PROP_BEEP
            AudioType.CHIME -> ToneGenerator.TONE_PROP_BEEP2
        }
        try {
            val toneGenerator = ToneGenerator(
                AudioAttributes.USAGE_ALARM,
                ToneGenerator.MAX_VOLUME / 2
            )
            toneGenerator.startTone(toneType, 150)
            // ToneGenerator holds native audio handles — must release after tone plays.
            // Post release on a background thread after the tone duration.
            Thread {
                Thread.sleep(200L) // slightly longer than tone duration
                toneGenerator.release()
            }.start()
        } catch (_: Exception) {
            // Audio may not be available — haptic is the priority, so fail silently
        }
    }

    fun cancel() {
        vibrator.cancel()
    }
}
