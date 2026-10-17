package com.jaroapps.dailybrain.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    onStartTraining: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenCalendar: () -> Unit,
    onStartMemoryTraining: () -> Unit,
    onStartStroopTest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            text = "Daily Brain",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Твоя ежедневная тренировка мозга",
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(Modifier.height(36.dp))

        Text(
            text = if (state.isTrainingCompletedToday) {
                "Тренировка выполнена"
            } else {
                "Сегодня ещё не тренировался"
            },
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .clickable(onClick = onOpenCalendar)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.lastSevenDays.forEach { day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = day.date.dayOfWeek.getDisplayName(
                            TextStyle.SHORT,
                            Locale("ru"),
                        ).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                    )

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                color = if (day.isCompleted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day.isCompleted) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.totalTrainingCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = "Завершённых тренировок",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = onOpenStatistics,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Моя статистика")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onStartTraining,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (state.isTrainingCompletedToday) {
                    "Тренироваться ещё"
                } else {
                    "Начать тренировку"
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onStartMemoryTraining,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Запоминание слов")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onStartStroopTest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Тест Струпа")
        }
    }
}
