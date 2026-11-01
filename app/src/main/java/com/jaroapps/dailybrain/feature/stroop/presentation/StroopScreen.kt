package com.jaroapps.dailybrain.feature.stroop.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.jaroapps.dailybrain.R
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.feature.stroop.domain.model.StroopStimulus
import com.jaroapps.dailybrain.core.presentation.ExerciseExitConfirmationDialog
import org.koin.androidx.compose.koinViewModel
import kotlin.math.ceil

@Composable
fun StroopScreen(
    onExit: () -> Unit,
    viewModel: StroopViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirmation by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAppResumed()
                Lifecycle.Event.ON_STOP -> viewModel.onAppPaused()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        if (
            lifecycleOwner.lifecycle.currentState.isAtLeast(
                Lifecycle.State.STARTED
            )
        ) {
            viewModel.onAppResumed()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onAppPaused()
        }
    }

    BackHandler {
        when {
            !state.isFinished -> showExitConfirmation = true
            state.isSaved -> onExit()
        }
    }

    if (state.isFinished) {
        StroopResultContent(
            elapsedTimeMs = state.elapsedTimeMs,
            isSaving = state.isSaving,
            isSaved = state.isSaved,
            errorMessage = state.errorMessage,
            onRetrySave = viewModel::retrySave,
            onExit = onExit,
        )
    } else {
        StroopTrainingContent(
            stimuli = state.stimuli,
            elapsedTimeMs = state.elapsedTimeMs,
            onFinish = viewModel::finishTraining,
            onExit = { showExitConfirmation = true },
        )
    }

    ExerciseExitConfirmationDialog(
        visible = showExitConfirmation,
        onDismiss = { showExitConfirmation = false },
        onConfirmExit = {
            showExitConfirmation = false
            onExit()
        },
    )
}

@Composable
private fun StroopTrainingContent(
    stimuli: List<StroopStimulus>,
    elapsedTimeMs: Long,
    onFinish: () -> Unit,
    onExit: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.screenWidthDp > configuration.screenHeightDp

    val columns = if (configuration.screenWidthDp >= 600) 8 else 4
    val longestWordLength = stimuli.maxOfOrNull { it.word.length } ?: 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.IconButton(onClick = onExit) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_close),
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = stringResource(R.string.exercise_stroop),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Text(
            text = stringResource(R.string.stroop_instruction),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        Text(
            text = formatElapsedTime(elapsedTimeMs),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val cellSpacing = 3.dp
            val cellWidth =
                (maxWidth - cellSpacing * (columns - 1)) / columns

            // One font size for the whole set; base it on the longest word.
            val fontSize = (
                    cellWidth.value / longestWordLength * 1.45f
                    ).coerceIn(10f, if (isLandscape) 22f else 24f).sp

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(cellSpacing),
            ) {
                stimuli.chunked(columns).forEach { rowStimuli ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                    ) {
                        rowStimuli.forEach { stimulus ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Color.Transparent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stimulus.word,
                                    color = Color(stimulus.inkColor.argb.toInt()),
                                    fontSize = fontSize,
                                    lineHeight = fontSize,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        // Keep the final row's cells the same width.
                        repeat(columns - rowStimuli.size) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.action_finish_test))
        }
    }
}

@Composable
private fun StroopResultContent(
    elapsedTimeMs: Long,
    isSaving: Boolean,
    isSaved: Boolean,
    errorMessage: String?,
    onRetrySave: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.status_test_completed),
            style = MaterialTheme.typography.headlineMedium,
        )

        Text(
            text = formatElapsedTime(elapsedTimeMs),
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        if (isSaving) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.status_saving),
                modifier = Modifier.padding(top = 12.dp),
            )
        } else if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )

            Button(
                onClick = onRetrySave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.action_retry_save))
            }
        } else {
            Text(
                text = stringResource(R.string.status_saved),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            enabled = isSaved,
        ) {
            Text(stringResource(R.string.action_done))
        }
    }
}

private fun formatElapsedTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (timeMs % 1_000) / 100

    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}