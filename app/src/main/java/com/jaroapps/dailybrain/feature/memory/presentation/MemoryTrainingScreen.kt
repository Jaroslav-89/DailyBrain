package com.jaroapps.dailybrain.feature.memory.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.feature.memory.domain.model.MemoryTrainingResult
import org.koin.androidx.compose.koinViewModel

@Composable
fun MemoryTrainingScreen(
    onExit: () -> Unit,
    viewModel: MemoryTrainingViewModel = koinViewModel()
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
        onExit()
    }

    when (val currentState = state) {
        MemoryTrainingUiState.Loading -> {
            LoadingContent()
        }

        is MemoryTrainingUiState.Memorizing -> {
            MemorizingContent(
                words = currentState.words.map { it.text },
                progress = currentState.progress,
                onExit = onExit
            )
        }

        is MemoryTrainingUiState.Recall -> {
            RecallContent(
                answers = currentState.answers,
                isSaving = currentState.isSaving,
                errorMessage = currentState.errorMessage,
                onAnswerChanged = viewModel::updateAnswer,
                onCheck = viewModel::checkAnswers,
                onExit = onExit
            )
        }

        is MemoryTrainingUiState.Result -> {
            ResultContent(
                result = currentState.result,
                onExit = onExit
            )
        }

        is MemoryTrainingUiState.Error -> {
            ErrorContent(
                message = currentState.message,
                onExit = onExit
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Подготавливаем слова…")
    }
}

@Composable
private fun MemorizingContent(
    words: List<String>,
    progress: Float,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onExit) {
                Text("Выйти")
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "Запоминание",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Запомни слова. У тебя 120 секунд.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(words) { index, word ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(0.dp))
                        Text(
                            text = word,
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        Text(
            text = "Полоса заполнится, когда время закончится.",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun RecallContent(
    answers: List<String>,
    isSaving: Boolean,
    errorMessage: String?,
    onAnswerChanged: (Int, String) -> Unit,
    onCheck: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onExit,
                enabled = !isSaving
            ) {
                Text("Выйти")
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Вспомни слова",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Введи вспомнившиеся слова в любые поля. " +
                    "Порядок не важен, пустые поля можно оставить.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(answers) { index, answer ->
                OutlinedTextField(
                    value = answer,
                    onValueChange = { onAnswerChanged(index, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Слово ${index + 1}") },
                    singleLine = true,
                    enabled = !isSaving
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = onCheck,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Сохраняем результат…",
                    modifier = Modifier.padding(start = 12.dp)
                )
            } else {
                Text("Проверить ответы")
            }
        }
    }
}

@Composable
private fun ResultContent(
    result: MemoryTrainingResult,
    onExit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Тренировка завершена!",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${result.correctCount} из ${result.shownWords.size}",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Точность: ${result.accuracyPercent}%",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Завершить")
            }
        }

        item {
            Text(
                text = "Вспомнил (${result.recalledWords.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (result.recalledWords.isEmpty()) {
            item { Text("Пока нет правильно вспомненных слов.") }
        } else {
            itemsIndexed(result.recalledWords) { _, word ->
                Text(
                    text = "✓  ${word.text}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Пропустил (${result.missedWords.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (result.missedWords.isEmpty()) {
            item { Text("Отлично! Ты вспомнил все слова.") }
        } else {
            itemsIndexed(result.missedWords) { _, word ->
                Text(
                    text = "•  ${word.text}",
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Не удалось начать тренировку",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(12.dp))
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onExit) {
            Text("Назад")
        }
    }
}
