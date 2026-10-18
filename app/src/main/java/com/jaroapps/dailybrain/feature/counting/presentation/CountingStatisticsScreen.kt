package com.jaroapps.dailybrain.feature.counting.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.core.utils.formatCountingTime
import com.jaroapps.dailybrain.feature.counting.domain.model.CountingStatistics
import org.koin.androidx.compose.koinViewModel

@Composable
fun CountingStatisticsScreen(
    onBack: () -> Unit,
    viewModel: CountingStatisticsViewModel = koinViewModel(),
) {
    val statistics by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Статистика устного счёта",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "За всё время",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        if (statistics.trainingCount == 0) {
            Text(
                text = "Пока нет завершённых тренировок",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        } else {
            CountingStatisticCard(
                title = "Всего тренировок",
                value = statistics.trainingCount.toString(),
            )

            Spacer(Modifier.height(12.dp))

            CountingStatisticCard(
                title = "Общее время",
                value = formatCountingTime(statistics.totalDurationMs),
            )

            Spacer(Modifier.height(12.dp))

            CountingStatisticCard(
                title = "Лучший результат",
                value = statistics.bestDurationMs
                    ?.let(::formatCountingTime)
                    ?: "—",
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Назад")
        }
    }
}

@Composable
private fun CountingStatisticCard(
    title: String,
    value: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}