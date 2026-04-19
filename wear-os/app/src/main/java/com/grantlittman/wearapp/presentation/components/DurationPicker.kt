package com.grantlittman.wearapp.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

/**
 * A compact duration picker for Wear OS with +/- buttons.
 * Displays as "Xm Ys" and allows stepping minutes and seconds independently.
 *
 * @param durationMillis Current duration in milliseconds.
 * @param onDurationChanged Callback when the user changes the duration.
 * @param minMillis Minimum allowed duration.
 * @param maxMillis Maximum allowed duration.
 * @param stepSeconds Step size for the seconds +/- buttons.
 * @param stepMinutes Step size for the minutes +/- buttons.
 */
@Composable
fun DurationPicker(
    durationMillis: Long,
    onDurationChanged: (Long) -> Unit,
    minMillis: Long = 1_000L,
    maxMillis: Long = 4 * 60 * 60 * 1_000L,
    stepSeconds: Int = 5,
    stepMinutes: Int = 1
) {
    val totalSeconds = (durationMillis / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minutes column
        StepperColumn(
            value = minutes,
            label = "m",
            onIncrement = {
                val newMillis = (durationMillis + stepMinutes * 60_000L).coerceAtMost(maxMillis)
                onDurationChanged(newMillis)
            },
            onDecrement = {
                val newMillis = (durationMillis - stepMinutes * 60_000L).coerceAtLeast(minMillis)
                onDurationChanged(newMillis)
            }
        )

        Text(
            text = ":",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        // Seconds column
        StepperColumn(
            value = seconds,
            label = "s",
            onIncrement = {
                val newMillis = (durationMillis + stepSeconds * 1_000L).coerceAtMost(maxMillis)
                onDurationChanged(newMillis)
            },
            onDecrement = {
                val newMillis = (durationMillis - stepSeconds * 1_000L).coerceAtLeast(minMillis)
                onDurationChanged(newMillis)
            }
        )
    }
}

@Composable
private fun StepperColumn(
    value: Int,
    label: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Button(
            onClick = onIncrement,
            modifier = Modifier.size(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            label = {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        )

        Text(
            text = "%02d%s".format(value, label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onDecrement,
            modifier = Modifier.size(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            label = {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        )
    }
}
