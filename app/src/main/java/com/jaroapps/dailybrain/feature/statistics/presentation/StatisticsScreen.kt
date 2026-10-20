package com.jaroapps.dailybrain.feature.statistics.presentation

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jaroapps.dailybrain.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPeriod by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        stringResource(R.string.statistics_tab_overall),
        stringResource(R.string.statistics_arithmetic),
        stringResource(R.string.statistics_memory),
        stringResource(R.string.exercise_counting),
        stringResource(R.string.exercise_stroop),
    )

    val stats = when (selectedTab) {
        1 -> state.arithmetic
        2 -> state.memoryWords
        3 -> state.countingTraining
        4 -> state.stroopTest
        else -> state.overall
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.statistics_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.statistics_tagline),
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(Modifier.height(20.dp))

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        StatisticCard(
            title = stringResource(R.string.statistics_total_trainings),
            value = stats.trainingCount.toString(),
        )

        Spacer(Modifier.height(12.dp))

        StatisticCard(
            title = stringResource(R.string.statistics_last_seven_days),
            value = stats.weeklyTrainingCount.toString(),
        )

        if (selectedTab == 0) {
            Spacer(Modifier.height(12.dp))
            StatisticCard(
                title = stringResource(R.string.statistics_current_streak),
                value = stringResource(R.string.statistics_days_count, state.currentStreakDays),
            )
            Spacer(Modifier.height(12.dp))
            StatisticCard(
                title = stringResource(R.string.statistics_best_streak),
                value = stringResource(R.string.statistics_days_count, state.bestStreakDays),
            )
        } else if (selectedTab == 1) {
            Spacer(Modifier.height(12.dp))
            StatisticCard(
                title = stringResource(R.string.statistics_best_time_95_accuracy),
                value = state.arithmeticBestDurationMs?.let { duration ->
                    formatDuration(duration)
                } ?: stringResource(R.string.statistics_no_record),
            )
        } else if (selectedTab == 2) {
            Spacer(Modifier.height(12.dp))
            StatisticCard(
                title = stringResource(R.string.statistics_memory_record),
                value = state.memoryBestCorrectCount?.toString()
                    ?: stringResource(R.string.statistics_no_record),
            )
        } else if (selectedTab == 3 || selectedTab == 4) {
            val bestDuration = if (selectedTab == 3) {
                state.countingBestDurationMs
            } else {
                state.stroopBestDurationMs
            }
            Spacer(Modifier.height(12.dp))
            StatisticCard(
                title = stringResource(R.string.statistics_best_time),
                value = if (bestDuration != null) {
                    formatDuration(bestDuration)
                } else {
                    stringResource(R.string.statistics_no_record)
                },
            )
        }

        if (selectedTab != 2) {
            Spacer(Modifier.height(12.dp))
            StatisticCard(
                title = stringResource(R.string.statistics_total_duration),
                value = formatDuration(stats.totalDurationMs),
            )
        }

        if (selectedTab != 0 && selectedTab != 2) {
            Spacer(Modifier.height(12.dp))
            StatisticCard(
                title = stringResource(R.string.statistics_average_duration),
                value = formatDuration(stats.averageDurationMs),
            )
        }

        if (selectedTab == 1) {
            Spacer(Modifier.height(12.dp))
            StatisticCard(
                title = stringResource(R.string.statistics_average_accuracy),
                value = formatPercent(stats.averageAccuracyPercent),
            )

            Spacer(Modifier.height(12.dp))
            StatisticCard(
                title = stringResource(R.string.statistics_best_accuracy),
                value = formatPercent(stats.bestAccuracyPercent),
            )
        }

        Spacer(Modifier.height(24.dp))


        if (selectedTab != 0) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.statistics_graph_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("7 дн.", "30 дн.", "6 мес.", "12 мес.", "Всё").forEachIndexed { index, label ->
                    OutlinedButton(
                        onClick = { selectedPeriod = index },
                        modifier = Modifier.weight(1f),
                    ) { Text(label, fontSize = 11.sp) }
                }
            }
            Spacer(Modifier.height(12.dp))
            val today = LocalDate.now()
            val start = when (selectedPeriod) {
                0 -> today.minusDays(6)
                1 -> today.minusDays(29)
                2 -> today.minusMonths(6).plusDays(1)
                3 -> today.minusMonths(12).plusDays(1)
                else -> null
            }
            val series = state.dailyResults.mapNotNull { day ->
                val value = when (selectedTab) {
                    1 -> day.arithmeticBestTimeMs?.toFloat()
                    2 -> day.memoryBestCorrectCount?.toFloat()
                    3 -> day.countingBestTimeMs?.toFloat()
                    else -> day.stroopBestTimeMs?.toFloat()
                }
                val date = runCatching {
                    LocalDate.parse(day.date.toString(), DateTimeFormatter.BASIC_ISO_DATE)
                }.getOrNull()
                if (date == null || value == null || (start != null && date.isBefore(start)) || date.isAfter(today)) null
                else DailyGraphPoint(date, value)
            }.sortedBy { it.date }
            DailyResultGraph(points = series, lowerIsBetter = selectedTab != 2)
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_home))
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
                modifier = Modifier.weight(1f),
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

@Composable
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> stringResource(R.string.statistics_duration_hours, hours, minutes)
        minutes > 0 -> stringResource(R.string.statistics_duration_minutes, minutes, seconds)
        else -> stringResource(R.string.statistics_duration_seconds, seconds)
    }
}


private data class DailyGraphPoint(val date: LocalDate, val value: Float)

@Composable
private fun DailyResultGraph(points: List<DailyGraphPoint>, lowerIsBetter: Boolean) {
    val lineColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = surfaceColor,
    ) {
        Column(Modifier.padding(16.dp)) {
            if (points.isEmpty()) {
                Text(
                    text = stringResource(R.string.statistics_graph_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            } else {
                val best = if (lowerIsBetter) points.minOf { it.value } else points.maxOf { it.value }
                Text(
                    text = stringResource(
                        R.string.statistics_graph_best,
                        if (lowerIsBetter) formatDuration(best.toLong()) else best.toInt().toString(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                    val left = 12.dp.toPx()
                    val right = size.width - 12.dp.toPx()
                    val top = 12.dp.toPx()
                    val bottom = size.height - 12.dp.toPx()
                    val minValue = points.minOf { it.value }
                    val maxValue = points.maxOf { it.value }
                    val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
                    val minDate = points.first().date
                    val maxDate = points.last().date
                    val dateRange = java.time.temporal.ChronoUnit.DAYS.between(minDate, maxDate).toFloat().coerceAtLeast(1f)
                    val coords = points.map { point ->
                        Offset(
                            x = left + java.time.temporal.ChronoUnit.DAYS.between(minDate, point.date) / dateRange * (right - left),
                            y = bottom - (point.value - minValue) / range * (bottom - top),
                        )
                    }
                    drawLine(textColor.copy(alpha = 0.35f), Offset(left, bottom), Offset(right, bottom), strokeWidth = 1.dp.toPx())
                    coords.forEachIndexed { index, point ->
                        if (index > 0 && points[index].date == points[index - 1].date.plusDays(1)) {
                            drawLine(lineColor, coords[index - 1], point, strokeWidth = 2.5.dp.toPx())
                        }
                        drawCircle(lineColor, radius = 4.dp.toPx(), center = point)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(points.first().date.format(DateTimeFormatter.ofPattern("dd.MM")), color = textColor, fontSize = 11.sp)
                    if (points.size > 1) Text(points.last().date.format(DateTimeFormatter.ofPattern("dd.MM")), color = textColor, fontSize = 11.sp)
                }
            }
        }
    }
}
