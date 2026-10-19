package com.jaroapps.dailybrain.feature.counting.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jaroapps.dailybrain.R
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.core.utils.formatCountingTime
import org.koin.androidx.compose.koinViewModel

@Composable
fun CountingTrainingScreen(
    onExit: () -> Unit,
    viewModel: CountingTrainingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
                Lifecycle.State.STARTED,
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
        if (!state.isFinished || state.isSaved) {
            onExit()
        }
    }

    if (state.isFinished) {
        CountingResultContent(
            elapsedTimeMs = state.elapsedTimeMs,
            isSaving = state.isSaving,
            isSaved = state.isSaved,
            errorMessage = state.errorMessage,
            onRetrySave = viewModel::retrySave,
            onExit = onExit,
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.exercise_counting),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.counting_instruction),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = formatCountingTime(state.elapsedTimeMs),
                style = MaterialTheme.typography.displayMedium,
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = viewModel::finishTraining,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_finish_training))
            }
        }
    }
}

@Composable
private fun CountingResultContent(
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
            text = stringResource(R.string.status_training_completed),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Text(
            text = formatCountingTime(elapsedTimeMs),
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        when {
            isSaving -> {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.status_saving),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )

                OutlinedButton(
                    onClick = onRetrySave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text(stringResource(R.string.action_retry_save))
                }
            }

            isSaved -> {
                Text(
                    text = stringResource(R.string.status_saved),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
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
