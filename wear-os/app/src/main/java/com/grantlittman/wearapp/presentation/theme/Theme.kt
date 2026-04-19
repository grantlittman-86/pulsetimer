package com.grantlittman.wearapp.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * PulseTimer color palette built on top of Wear M3 defaults.
 * Primary: cyan-blue for time displays and primary actions.
 * Tertiary: warm amber for milestones and hints.
 * Error: red-orange for countdown warnings and stop buttons.
 */
private val PulseTimerColors = ColorScheme().copy(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF003547),
    primaryContainer = Color(0xFF004D6E),
    onPrimaryContainer = Color(0xFFBDE9FF),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF00504B),
    onSecondaryContainer = Color(0xFFA0F0E8),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF462B00),
    tertiaryContainer = Color(0xFF633F00),
    onTertiaryContainer = Color(0xFFFFDDB3),
    error = Color(0xFFFF7043),
    onError = Color(0xFF601400),
    errorContainer = Color(0xFF7F2000),
    onErrorContainer = Color(0xFFFFDBD0)
)

@Composable
fun WearAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PulseTimerColors,
        content = content
    )
}
