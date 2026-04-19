package com.grantlittman.wearapp.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.grantlittman.wearapp.timer.TimerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

// Colors for signal flash feedback
private val FlashInterval = Color(0xFF2196F3).copy(alpha = 0.25f)   // blue tint
private val FlashMilestone = Color(0xFFFF9800).copy(alpha = 0.35f)  // orange tint
private val FlashCountdown = Color(0xFFF44336).copy(alpha = 0.30f)  // red tint
private val NoFlash = Color.Transparent

/**
 * Active timer screen shown while a pattern is running.
 * Supports two modes:
 * - Interactive: full UI with controls, colors, signal flash feedback
 * - Ambient: stripped-down, low-power display (white on black, no buttons, no animations)
 */
@Composable
fun TimerScreen(
    stateFlow: StateFlow<TimerState>,
    isAmbient: Boolean = false,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val state by stateFlow.collectAsState()

    if (isAmbient) {
        AmbientTimerLayout(state)
    } else {
        InteractiveTimerLayout(state, onPause, onResume, onStop)
    }
}

/**
 * Ambient (always-on) layout.
 * Rules for low power: white/gray on pure black, no animations, minimal updates.
 */
@Composable
private fun AmbientTimerLayout(state: TimerState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pattern name
        Text(
            text = state.patternName ?: "",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF999999),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Time display
        val displayFinished = state.isFinished ||
            (state.totalDurationMillis != null && (state.remainingMillis ?: 1L) <= 0L)

        if (displayFinished && state.totalDurationMillis != null) {
            Text(
                text = formatTime(state.totalDurationMillis),
                fontSize = 40.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "completed",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        } else if (state.totalDurationMillis != null) {
            Text(
                text = formatTime(state.remainingMillis ?: 0L),
                fontSize = 40.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "remaining",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        } else {
            Text(
                text = formatTime(state.elapsedMillis),
                fontSize = 40.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "elapsed",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }

        // Next milestone hint
        state.nextMilestone?.let { milestone ->
            val timeUntil = milestone.triggerAtMillis - state.elapsedMillis
            if (timeUntil > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                val label = milestone.label ?: "Next"
                Text(
                    text = "$label in ${formatDurationShort(timeUntil)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF999999),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (state.isFinished) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "DONE",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        if (state.isPaused) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "PAUSED",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF999999),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Interactive (screen-on) layout with full colors, controls, and signal flash.
 */
@Composable
private fun InteractiveTimerLayout(
    state: TimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    // Signal flash: when lastSignalFiredAt changes, briefly flash the background
    var flashActive by remember { mutableStateOf(false) }
    val lastFired = state.lastSignalFiredAt

    LaunchedEffect(lastFired) {
        if (lastFired > 0L) {
            flashActive = true
            delay(400L)
            flashActive = false
        }
    }

    val flashColor = when {
        !flashActive -> NoFlash
        state.inCountdown -> FlashCountdown
        state.lastSignalWasMilestone -> FlashMilestone
        else -> FlashInterval
    }

    val backgroundColor by animateColorAsState(
        targetValue = flashColor,
        animationSpec = tween(durationMillis = if (flashActive) 100 else 300),
        label = "signalFlash"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pattern name
            Text(
                text = state.patternName ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Main time display
            val timeColor = when {
                state.isFinished -> MaterialTheme.colorScheme.error
                state.inCountdown -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }

            val displayFinished = state.isFinished ||
                (state.totalDurationMillis != null && (state.remainingMillis ?: 1L) <= 0L)

            if (displayFinished && state.totalDurationMillis != null) {
                // Timer completed — show total duration that was run
                Text(
                    text = formatTime(state.totalDurationMillis),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = timeColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (state.totalDurationMillis != null) {
                Text(
                    text = formatTime(state.remainingMillis ?: 0L),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = timeColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = formatTime(state.elapsedMillis),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = timeColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "elapsed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress indicator for timed patterns
            state.totalDurationMillis?.let { total ->
                if (total > 0 && !state.isFinished) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val progress = (state.elapsedMillis.toFloat() / total).coerceIn(0f, 1f)
                    val pct = (progress * 100).toInt()
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Next milestone hint
            state.nextMilestone?.let { milestone ->
                val timeUntil = milestone.triggerAtMillis - state.elapsedMillis
                if (timeUntil > 0) {
                    val label = milestone.label ?: "Next"
                    Text(
                        text = "$label in ${formatDurationShort(timeUntil)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Finished state
            if (state.isFinished) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Done!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isFinished) {
                    Button(
                        onClick = onStop,
                        label = { Text("Done") }
                    )
                } else if (state.isPaused) {
                    Button(
                        onClick = onResume,
                        label = { Text("Resume") }
                    )
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        label = { Text("Stop") }
                    )
                } else if (state.isRunning) {
                    Button(
                        onClick = onPause,
                        label = { Text("Pause") }
                    )
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        label = { Text("Stop") }
                    )
                }
            }
        }
    }
}

/**
 * Formats milliseconds into MM:SS or H:MM:SS display.
 */
private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
