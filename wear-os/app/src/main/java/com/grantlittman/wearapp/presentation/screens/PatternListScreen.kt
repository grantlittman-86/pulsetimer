package com.grantlittman.wearapp.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import com.grantlittman.wearapp.data.model.Pattern
import kotlinx.coroutines.flow.Flow

/**
 * Home screen showing all saved patterns and presets.
 * Sorted: recently used first, then presets, then alphabetical.
 */
@Composable
fun PatternListScreen(
    patternsFlow: Flow<List<Pattern>>,
    onPatternSelected: (Pattern) -> Unit,
    onPatternEdit: (Pattern) -> Unit,
    onCreateNew: () -> Unit
) {
    val rawPatterns by patternsFlow.collectAsState(initial = emptyList())
    val listState = rememberScalingLazyListState()

    // Sort: recently used first, then presets, then by name
    val patterns = rawPatterns.sortedWith(
        compareByDescending<Pattern> { it.lastUsedAt ?: 0L }
            .thenByDescending { it.isPreset }
            .thenBy { it.name }
    )

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Header
        item {
            Text(
                text = "PulseTimer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 4.dp)
            )
        }

        // Create new button
        item {
            Button(
                onClick = onCreateNew,
                label = { Text("+ New Pattern") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (patterns.isEmpty()) {
            item {
                Text(
                    text = "Loading patterns...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(patterns, key = { it.id }) { pattern ->
                PatternCard(
                    pattern = pattern,
                    onStart = { onPatternSelected(pattern) },
                    onEdit = { onPatternEdit(pattern) }
                )
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PatternCard(
    pattern: Pattern,
    onStart: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(4.dp)
        ) {
            // Top row: name + badges + edit
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pattern name (defensive null check — Gson can bypass Kotlin non-null)
                Text(
                    text = pattern.name ?: "Untitled",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Badges
                if (pattern.isPreset) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PRESET",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                TextButton(onClick = onEdit) {
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Detail summary — compact single line
            Text(
                text = formatPatternSummary(pattern),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Builds a compact summary like "5 min \u00B7 every 1 min \u00B7 2 milestones"
 */
private fun formatPatternSummary(pattern: Pattern): String {
    val parts = mutableListOf<String>()

    pattern.totalDurationMillis?.let {
        parts.add(formatDurationShort(it))
    } ?: parts.add("No limit")

    // Defensive: repeatingInterval could be null at runtime via Gson
    pattern.repeatingInterval?.let {
        parts.add("every ${formatDurationShort(it.intervalMillis)}")
    }

    val milestones = pattern.milestones
    if (milestones != null && milestones.isNotEmpty()) {
        val count = milestones.size
        parts.add("${count}ms") // short for milestones
    }

    return parts.joinToString(" \u00B7 ")
}

/**
 * Formats milliseconds into a short human-readable string.
 */
internal fun formatDurationShort(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return when {
        minutes >= 60 -> {
            val hours = minutes / 60
            val remainingMin = minutes % 60
            if (remainingMin > 0) "${hours}h ${remainingMin}m" else "${hours}h"
        }
        minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes} min"
        else -> "${seconds}s"
    }
}
