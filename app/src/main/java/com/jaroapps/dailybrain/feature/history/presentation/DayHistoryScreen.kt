package com.jaroapps.dailybrain.feature.history.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.ZoneId

@Composable
fun DayHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayHistoryViewModel = koinViewModel(),
) {
    val sessions by viewModel.sessions.collectAsState()
    val date = viewModel.date

    val dateFormatter = DateTimeFormatter
        .ofPattern("d MMMM yyyy", Locale("ru"))
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val zone = ZoneId.systemDefault()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("‹ Назад")
        }

        Text(
            text = date.format(dateFormatter)
                .replaceFirstChar { it.uppercase(Locale("ru")) },
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.size(8.dp))

        Text(
            text = when (sessions.size) {
                0 -> "Тренировок нет"
                1 -> "1 тренировка"
                in 2..4 -> "${sessions.size} тренировки"
                else -> "${sessions.size} тренировок"
            },
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(Modifier.size(16.dp))

        if (sessions.isEmpty()) {
            Text(
                text = "В этот день ещё нет завершённых тренировок.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = sessions,
                    key = { it.id },
                ) { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(
                                text = when (session.exerciseType) {
                            "ARITHMETIC" -> "Арифметика"
                            "MEMORY_WORDS" -> "Запоминание слов"
                            else -> session.exerciseType
                        },
                                style = MaterialTheme.typography.titleMedium,
                            )

                            Spacer(Modifier.size(8.dp))

                            val finishedTime = Instant
                                .ofEpochMilli(session.finishedAtEpochMs)
                                .atZone(zone)
                                .format(timeFormatter)

                            Text("Завершена в $finishedTime")

                            Spacer(Modifier.size(4.dp))

                            Text("Длительность: ${formatDuration(session.durationMs)}")

                            Spacer(Modifier.size(4.dp))

                            Text(
                                "Точность: ${
                                    session.accuracyPercent
                                        .let { String.format(Locale.getDefault(), "%.0f", it) }
                                }%"
                            )

                            Spacer(Modifier.size(4.dp))

                            Text(
                                "Правильных ответов: " +
                                        "${session.correctCount} из ${session.taskCount}"
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return if (minutes > 0) {
        "${minutes} мин ${seconds} сек"
    } else {
        "${seconds} сек"
    }
}
