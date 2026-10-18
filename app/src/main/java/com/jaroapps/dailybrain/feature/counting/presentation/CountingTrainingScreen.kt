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
    onOpenStatistics: () -> Unit,
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

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(
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
        onExit()
    }

    if (state.isFinished) {
        CountingResultContent(
            elapsedTimeMs = state.elapsedTimeMs,
            isSaving = state.isSaving,
            isSaved = state.isSaved,
            errorMessage = state.errorMessage,
            onRetrySave = viewModel::retrySave,
            onOpenStatistics = onOpenStatistics,
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
                text = "Устный счёт",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Считай вслух от 1 до 120 как можно быстрее.",
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
                Text("Завершить тренировку")
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
    onOpenStatistics: () -> Unit,
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
            text = "Тренировка завершена",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Время тренировки",
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = formatCountingTime(elapsedTimeMs),
            style = MaterialTheme.typography.displayMedium,
        )

        Spacer(Modifier.height(24.dp))

        when {
            isSaving -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Сохраняем результат…")
            }

            isSaved -> {
                Text(
                    text = "Результат сохранён",
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onRetrySave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Повторить сохранение")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onOpenStatistics,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Статистика упражнения")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Завершить")
        }
    }
}

