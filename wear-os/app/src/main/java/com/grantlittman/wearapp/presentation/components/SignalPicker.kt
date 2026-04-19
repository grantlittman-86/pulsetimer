package com.grantlittman.wearapp.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import com.grantlittman.wearapp.data.model.HapticType

/**
 * Compact signal type selector for Wear OS.
 * Shows the current haptic type with left/right cycle buttons on one row,
 * and a "Try" button below.
 */
@Composable
fun SignalPicker(
    selectedType: HapticType,
    onTypeChanged: (HapticType) -> Unit,
    onTrySignal: (HapticType) -> Unit
) {
    val types = HapticType.entries
    val currentIndex = types.indexOf(selectedType)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Carousel row: < Type Name >
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous
            TextButton(
                onClick = {
                    val prev = if (currentIndex > 0) currentIndex - 1 else types.lastIndex
                    onTypeChanged(types[prev])
                }
            ) {
                Text("<")
            }

            // Current type name — fixed min width so the row doesn't jump around
            Text(
                text = hapticTypeLabel(selectedType),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(min = 80.dp)
                    .padding(horizontal = 4.dp)
            )

            // Next
            TextButton(
                onClick = {
                    val next = if (currentIndex < types.lastIndex) currentIndex + 1 else 0
                    onTypeChanged(types[next])
                }
            ) {
                Text(">")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Try button on its own row
        Button(
            onClick = { onTrySignal(selectedType) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            ),
            label = {
                Text(
                    text = "Try",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )
    }
}

fun hapticTypeLabel(type: HapticType): String = when (type) {
    HapticType.TAP -> "Tap"
    HapticType.BUZZ -> "Buzz"
    HapticType.PULSE -> "Pulse"
    HapticType.DOUBLE_TAP -> "Double Tap"
}
