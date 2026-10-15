package com.jaroapps.dailybrain.feature.statistics.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = "Статистика",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Твой прогресс в Daily Brain",
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(Modifier.height(24.dp))

        StatisticCard(
            title = "Всего тренировок",
            value = state.totalTrainingCount.toString(),
        )

        Spacer(Modifier.height(12.dp))

        StatisticCard(
            title = "За последние 7 дней",
            value = state.weeklyTrainingCount.toString(),
        )

        Spacer(Modifier.height(12.dp))

        StatisticCard(
            title = "Средняя точность",
            value = formatPercent(state.averageAccuracyPercent),
        )

        Spacer(Modifier.height(12.dp))

        StatisticCard(
            title = "Лучшая точность",
            value = formatPercent(state.bestAccuracyPercent),
        )

        Spacer(Modifier.height(12.dp))

        StatisticCard(
            title = "Средняя длительность",
            value = formatDuration(state.averageDurationMs),
        )

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("На главный экран")
        }
    }
}

@Composable
private fun StatisticCard(
    title: String,
    value: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun formatPercent(value: Double): String =
    String.format(Locale.getDefault(), "%.1f%%", value)

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return if (minutes > 0) {
        "${minutes} мин ${seconds} с"
    } else {
        "${seconds} с"
    }
}
