package com.jaroapps.dailybrain.feature.numbergrid.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.core.presentation.ExerciseExitConfirmationDialog
import com.jaroapps.dailybrain.feature.numbergrid.domain.model.NumberGridRules
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun NumberGridScreen(
    onExit: () -> Unit,
    viewModel: NumberGridViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showExitDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onAppResumed()
                Lifecycle.Event.ON_PAUSE -> viewModel.onAppPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.onAppResumed()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onAppPaused()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.startTraining(NumberGridRules.DEFAULT_GRID_SIZE)
    }

    BackHandler {
        if (state.isSaved) {
            onExit()
        } else if (!state.isCompleted) {
            showExitDialog = true
        }
    }

    ExerciseExitConfirmationDialog(
        visible = showExitDialog && !state.isCompleted,
        onDismiss = { showExitDialog = false },
        onConfirmExit = {
            showExitDialog = false
            viewModel.cancelTraining()
            onExit()
        },
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (state.isSaved) onExit()
                    else if (!state.isCompleted) showExitDialog = true
                },
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
            }
            Text(
                text = stringResource(R.string.exercise_number_grid),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))

        if (!state.isStarted) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (!state.isCompleted) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        stringResource(R.string.number_grid_find_number),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        state.expectedNumber.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorResource(R.color.color_button_green),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatGridDuration(state.elapsedTimeMs),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.number_grid_errors, state.errorCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(
                            R.string.number_grid_accuracy,
                            formatPercent(NumberGridRules.accuracyPercent(state.errorCount, state.totalCellCount)),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth(),
                color = colorResource(R.color.color_button_green),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                state.numbers.chunked(state.gridSize).forEach { rowNumbers ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        rowNumbers.forEach { number ->
                            val isLastCorrect = state.lastCorrectNumber == number
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isLastCorrect) colorResource(R.color.color_button_green)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    .clickable(enabled = state.isStarted) {
                                        viewModel.onNumberSelected(number)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = number.toString(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLastCorrect) colorResource(R.color.color_on_accent)
                                    else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.rules_number_grid),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        stringResource(R.string.number_grid_result_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(
                            R.string.number_grid_result_time,
                            formatGridDuration(state.durationMs ?: state.elapsedTimeMs),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(
                            R.string.number_grid_result_accuracy,
                            formatPercent(state.accuracyPercent ?: 0.0),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.number_grid_result_errors, state.errorCount),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    when {
                        state.isSaving -> Text(stringResource(R.string.status_saving))
                        state.isSaved -> Text(
                            stringResource(R.string.status_saved),
                            color = colorResource(R.color.color_button_green),
                        )
                        state.saveErrorMessage != null -> Text(
                            state.saveErrorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (!state.isSaved && !state.isSaving) {
                        Button(
                            onClick = viewModel::retrySave,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.color_button_green),
                                contentColor = colorResource(R.color.color_on_accent),
                            ),
                        ) {
                            Text(stringResource(R.string.action_retry_save))
                        }
                    }
                    Button(
                        onClick = { viewModel.startTraining(NumberGridRules.DEFAULT_GRID_SIZE) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.isSaved,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.color_button_green),
                            contentColor = colorResource(R.color.color_on_accent),
                        ),
                    ) {
                        Text(stringResource(R.string.number_grid_retry))
                    }
                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.isSaved,
                    ) {
                        Text(stringResource(R.string.number_grid_back_to_games))
                    }
                }
            }
        }
    }
}

@Composable
private fun formatGridDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) {
        stringResource(R.string.statistics_duration_minutes, minutes, seconds)
    } else {
        stringResource(R.string.statistics_duration_seconds, seconds)
    }
}

private fun formatPercent(value: Double): String =
    String.format(Locale.getDefault(), "%.0f", value.coerceIn(0.0, 100.0))
