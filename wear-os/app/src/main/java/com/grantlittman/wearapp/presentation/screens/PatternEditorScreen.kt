package com.grantlittman.wearapp.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import com.grantlittman.wearapp.data.model.CountdownWarning
import com.grantlittman.wearapp.data.model.HapticType
import com.grantlittman.wearapp.data.model.Milestone
import com.grantlittman.wearapp.data.model.Pattern
import com.grantlittman.wearapp.data.model.RepeatingInterval
import com.grantlittman.wearapp.data.model.Signal
import com.grantlittman.wearapp.presentation.components.DurationPicker
import com.grantlittman.wearapp.presentation.components.SignalPicker

/**
 * Pattern editor screen. Allows creating or editing a pattern.
 * Scrollable form with sections for each configurable aspect.
 *
 * @param existingPattern If non-null, we're editing this pattern. If null, creating a new one.
 * @param onSave Called with the completed pattern to save.
 * @param onDelete Called to delete the pattern (only shown for non-presets).
 * @param onCancel Called when the user cancels editing.
 * @param onTrySignal Called when user taps "Try" on a signal picker.
 */
@Composable
fun PatternEditorScreen(
    existingPattern: Pattern?,
    onSave: (Pattern) -> Unit,
    onDelete: ((Pattern) -> Unit)? = null,
    onCancel: () -> Unit,
    onTrySignal: (HapticType) -> Unit
) {
    val listState = rememberScalingLazyListState()

    // -- Editable state, initialized from existing pattern or defaults --
    var name by remember {
        mutableStateOf(existingPattern?.name ?: "My Pattern")
    }
    var hasDuration by remember {
        mutableStateOf(existingPattern?.totalDurationMillis != null)
    }
    var totalDurationMillis by remember {
        mutableLongStateOf(existingPattern?.totalDurationMillis ?: (10 * 60 * 1000L))
    }
    var intervalMillis by remember {
        mutableLongStateOf(existingPattern?.repeatingInterval?.intervalMillis ?: 60_000L)
    }
    var intervalHaptic by remember {
        mutableStateOf(existingPattern?.repeatingInterval?.signal?.hapticType ?: HapticType.TAP)
    }
    var audioEnabled by remember {
        mutableStateOf(existingPattern?.audioEnabled ?: false)
    }

    // Milestones
    val milestones = remember {
        mutableStateListOf<EditableMilestone>().apply {
            existingPattern?.milestones?.forEach { m ->
                add(
                    EditableMilestone(
                        triggerAtMillis = m.triggerAtMillis,
                        hapticType = m.signal.hapticType,
                        repeatCount = m.repeatCount,
                        label = m.label ?: ""
                    )
                )
            }
        }
    }

    // Countdown warning
    var hasCountdown by remember {
        mutableStateOf(existingPattern?.countdownWarning != null)
    }
    var countdownActivateMillis by remember {
        mutableLongStateOf(existingPattern?.countdownWarning?.activateAtMillis ?: 120_000L)
    }
    var countdownIntervalMillis by remember {
        mutableLongStateOf(existingPattern?.countdownWarning?.intervalMillis ?: 30_000L)
    }
    var countdownHaptic by remember {
        mutableStateOf(
            existingPattern?.countdownWarning?.signal?.hapticType ?: HapticType.BUZZ
        )
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // -- Header --
        item {
            Text(
                text = if (existingPattern != null) "Edit Pattern" else "New Pattern",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 4.dp)
            )
        }

        // -- Name --
        item {
            SectionHeader("Name")
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cycle through some preset names on watch (text input is hard on Wear)
                val baseNames = listOf(
                    "My Pattern", "Talk", "Keynote", "Workshop",
                    "Interview", "Meeting", "Workout", "Practice"
                )
                // If current name isn't in the list, include it so it's not lost
                val nameOptions = if (name in baseNames) baseNames
                    else listOf(name) + baseNames
                val currentIndex = nameOptions.indexOf(name).coerceAtLeast(0)

                Button(
                    onClick = {
                        val prev = if (currentIndex > 0) currentIndex - 1 else nameOptions.lastIndex
                        name = nameOptions[prev]
                    },
                    modifier = Modifier.padding(end = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    label = { Text("<") }
                )

                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        val next = if (currentIndex < nameOptions.lastIndex) currentIndex + 1 else 0
                        name = nameOptions[next]
                    },
                    modifier = Modifier.padding(start = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    label = { Text(">") }
                )
            }
        }

        // -- Total Duration --
        item {
            SectionHeader("Duration")
        }
        item {
            SwitchButton(
                checked = hasDuration,
                onCheckedChange = { hasDuration = it },
                label = { Text("Set time limit") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (hasDuration) {
            item {
                DurationPicker(
                    durationMillis = totalDurationMillis,
                    onDurationChanged = { totalDurationMillis = it },
                    minMillis = 60_000L,
                    stepMinutes = 1
                )
            }
        }

        // -- Repeating Interval --
        item {
            SectionHeader("Repeating Signal")
        }
        item {
            Text(
                text = "Interval",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            DurationPicker(
                durationMillis = intervalMillis,
                onDurationChanged = { intervalMillis = it },
                minMillis = 5_000L,
                stepSeconds = 5
            )
        }
        item {
            Text(
                text = "Signal type",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            SignalPicker(
                selectedType = intervalHaptic,
                onTypeChanged = { intervalHaptic = it },
                onTrySignal = onTrySignal
            )
        }

        // -- Milestones --
        item {
            SectionHeader("Milestones")
        }

        // Existing milestones
        milestones.forEachIndexed { index, milestone ->
            item(key = "milestone_$index") {
                MilestoneRow(
                    milestone = milestone,
                    onUpdate = { milestones[index] = it },
                    onRemove = { milestones.removeAt(index) },
                    onTrySignal = onTrySignal
                )
            }
        }

        item {
            Button(
                onClick = {
                    milestones.add(
                        EditableMilestone(
                            triggerAtMillis = totalDurationMillis / 2,
                            hapticType = HapticType.DOUBLE_TAP,
                            repeatCount = 1,
                            label = ""
                        )
                    )
                },
                label = { Text("+ Add Milestone") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // -- Countdown Warning (only available with a set duration) --
        if (hasDuration) {
            item {
                SectionHeader("Countdown Warning")
            }
            item {
                SwitchButton(
                    checked = hasCountdown,
                    onCheckedChange = { hasCountdown = it },
                    label = { Text("Enable countdown") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (hasCountdown && hasDuration) {
            item {
                Text(
                    text = "Activate before end",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                DurationPicker(
                    durationMillis = countdownActivateMillis,
                    onDurationChanged = { countdownActivateMillis = it },
                    minMillis = 30_000L,
                    stepSeconds = 30
                )
            }
            item {
                Text(
                    text = "Signal every",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                DurationPicker(
                    durationMillis = countdownIntervalMillis,
                    onDurationChanged = { countdownIntervalMillis = it },
                    minMillis = 5_000L,
                    stepSeconds = 5
                )
            }
            item {
                SignalPicker(
                    selectedType = countdownHaptic,
                    onTypeChanged = { countdownHaptic = it },
                    onTrySignal = onTrySignal
                )
            }
        }

        // -- Audio Toggle --
        item {
            SectionHeader("Audio")
        }
        item {
            SwitchButton(
                checked = audioEnabled,
                onCheckedChange = { audioEnabled = it },
                label = { Text("Enable sound") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // -- Save / Cancel / Delete --
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            Button(
                onClick = {
                    val pattern = buildPattern(
                        existingPattern = existingPattern,
                        name = name,
                        hasDuration = hasDuration,
                        totalDurationMillis = totalDurationMillis,
                        intervalMillis = intervalMillis,
                        intervalHaptic = intervalHaptic,
                        milestones = milestones,
                        hasCountdown = hasCountdown,
                        countdownActivateMillis = countdownActivateMillis,
                        countdownIntervalMillis = countdownIntervalMillis,
                        countdownHaptic = countdownHaptic,
                        audioEnabled = audioEnabled
                    )
                    onSave(pattern)
                },
                label = { Text("Save") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = onCancel,
                label = { Text("Cancel") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Delete button with confirmation (only for non-preset existing patterns)
        if (existingPattern != null && !existingPattern.isPreset && onDelete != null) {
            item {
                var confirmingDelete by remember { mutableStateOf(false) }

                if (confirmingDelete) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Delete this pattern?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { confirmingDelete = false },
                                label = { Text("Keep") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onDelete(existingPattern) },
                                label = { Text("Delete") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { confirmingDelete = true },
                        label = { Text("Delete") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -- Helper composables and data --

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp)
    )
}

/**
 * Mutable version of Milestone for use in the editor.
 */
data class EditableMilestone(
    val triggerAtMillis: Long,
    val hapticType: HapticType,
    val repeatCount: Int,
    val label: String
)

@Composable
private fun MilestoneRow(
    milestone: EditableMilestone,
    onUpdate: (EditableMilestone) -> Unit,
    onRemove: () -> Unit,
    onTrySignal: (HapticType) -> Unit
) {
    Card(
        onClick = {},
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "At ${formatDurationShort(milestone.triggerAtMillis)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = onRemove,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    label = { Text("X", style = MaterialTheme.typography.labelSmall) }
                )
            }

            DurationPicker(
                durationMillis = milestone.triggerAtMillis,
                onDurationChanged = { onUpdate(milestone.copy(triggerAtMillis = it)) },
                minMillis = 5_000L,
                stepSeconds = 15
            )

            SignalPicker(
                selectedType = milestone.hapticType,
                onTypeChanged = { onUpdate(milestone.copy(hapticType = it)) },
                onTrySignal = onTrySignal
            )

            // Repeat count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Repeat: ${milestone.repeatCount}x",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Button(
                    onClick = {
                        onUpdate(milestone.copy(repeatCount = (milestone.repeatCount - 1).coerceAtLeast(1)))
                    },
                    modifier = Modifier.size(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    label = { Text("-") }
                )
                Button(
                    onClick = {
                        onUpdate(milestone.copy(repeatCount = (milestone.repeatCount + 1).coerceAtMost(5)))
                    },
                    modifier = Modifier.size(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    label = { Text("+") }
                )
            }
        }
    }
}

/**
 * Assembles a Pattern from the editor's current state.
 */
private fun buildPattern(
    existingPattern: Pattern?,
    name: String,
    hasDuration: Boolean,
    totalDurationMillis: Long,
    intervalMillis: Long,
    intervalHaptic: HapticType,
    milestones: List<EditableMilestone>,
    hasCountdown: Boolean,
    countdownActivateMillis: Long,
    countdownIntervalMillis: Long,
    countdownHaptic: HapticType,
    audioEnabled: Boolean
): Pattern {
    return Pattern(
        id = existingPattern?.id ?: java.util.UUID.randomUUID().toString(),
        name = name,
        isPreset = existingPattern?.isPreset ?: false,
        totalDurationMillis = if (hasDuration) totalDurationMillis else null,
        repeatingInterval = RepeatingInterval(
            intervalMillis = intervalMillis,
            signal = Signal(hapticType = intervalHaptic)
        ),
        milestones = milestones.map { m ->
            Milestone(
                triggerAtMillis = m.triggerAtMillis,
                signal = Signal(hapticType = m.hapticType),
                repeatCount = m.repeatCount,
                label = m.label.ifBlank { null }
            )
        },
        countdownWarning = if (hasCountdown && hasDuration) {
            CountdownWarning(
                activateAtMillis = countdownActivateMillis,
                intervalMillis = countdownIntervalMillis,
                signal = Signal(hapticType = countdownHaptic)
            )
        } else null,
        audioEnabled = audioEnabled,
        lastUsedAt = existingPattern?.lastUsedAt
    )
}
