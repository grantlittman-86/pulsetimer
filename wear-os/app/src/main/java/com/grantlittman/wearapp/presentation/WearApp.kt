package com.grantlittman.wearapp.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.grantlittman.wearapp.data.model.HapticType
import com.grantlittman.wearapp.data.model.Pattern
import com.grantlittman.wearapp.data.repository.PatternRepository
import com.grantlittman.wearapp.presentation.navigation.Routes
import com.grantlittman.wearapp.presentation.screens.PatternEditorScreen
import com.grantlittman.wearapp.presentation.screens.PatternListScreen
import com.grantlittman.wearapp.presentation.screens.TimerScreen
import com.grantlittman.wearapp.timer.TimerService
import kotlinx.coroutines.launch

/**
 * Root composable that hosts navigation between all screens.
 */
@Composable
fun WearApp(
    repository: PatternRepository,
    timerBinder: TimerService.TimerBinder?,
    isAmbient: Boolean = false,
    onStartTimer: (Pattern) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onTrySignal: (HapticType) -> Unit
) {
    val navController = rememberSwipeDismissableNavController()
    val coroutineScope = rememberCoroutineScope()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Routes.PATTERN_LIST
    ) {
        // -- Home: Pattern List --
        composable(Routes.PATTERN_LIST) {
            PatternListScreen(
                patternsFlow = repository.allPatterns,
                onPatternSelected = { pattern ->
                    onStartTimer(pattern)
                    navController.navigate(Routes.TIMER) {
                        launchSingleTop = true
                    }
                },
                onPatternEdit = { pattern ->
                    navController.navigate(Routes.editorEdit(pattern.id)) {
                        launchSingleTop = true
                    }
                },
                onCreateNew = {
                    navController.navigate(Routes.EDITOR_NEW) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // -- Timer --
        composable(Routes.TIMER) {
            val binder = timerBinder
            if (binder != null) {
                TimerScreen(
                    stateFlow = binder.state,
                    isAmbient = isAmbient,
                    onPause = onPauseTimer,
                    onResume = onResumeTimer,
                    onStop = {
                        // Navigate away first so the screen doesn't flash the reset state
                        navController.popBackStack(Routes.PATTERN_LIST, inclusive = false)
                        onStopTimer()
                    }
                )
            } else {
                // Service not yet bound — show loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Starting...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // -- Editor: New Pattern --
        composable(Routes.EDITOR_NEW) {
            PatternEditorScreen(
                existingPattern = null,
                onSave = { pattern ->
                    coroutineScope.launch {
                        repository.save(pattern)
                    }
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                },
                onTrySignal = onTrySignal
            )
        }

        // -- Editor: Edit Existing Pattern --
        composable(
            route = Routes.EDITOR_EDIT,
            arguments = listOf(navArgument("patternId") { type = NavType.StringType })
        ) { backStackEntry ->
            val patternId = backStackEntry.arguments?.getString("patternId")

            // Load pattern from repo
            val pattern = remember(patternId) {
                mutableStateOf<Pattern?>(null)
            }
            LaunchedEffect(patternId) {
                patternId?.let { pattern.value = repository.getById(it) }
            }

            pattern.value?.let { existingPattern ->
                PatternEditorScreen(
                    existingPattern = existingPattern,
                    onSave = { updatedPattern ->
                        coroutineScope.launch {
                            repository.save(updatedPattern)
                        }
                        navController.popBackStack()
                    },
                    onDelete = { patternToDelete ->
                        coroutineScope.launch {
                            repository.delete(patternToDelete)
                        }
                        navController.popBackStack()
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                    onTrySignal = onTrySignal
                )
            }
        }
    }
}
