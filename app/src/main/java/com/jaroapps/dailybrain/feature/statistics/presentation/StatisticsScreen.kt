package com.jaroapps.dailybrain.feature.statistics.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.jaroapps.dailybrain.core.database.entity.DailyExerciseResultEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    var selectedTab by remember { mutableIntStateOf(0) }

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

        if (selectedTab != 0) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.statistics_progress_chart),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ChartPeriod.values().forEach { period ->
                    OutlinedButton(
                        onClick = { viewModel.selectChartPeriod(period) },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = when (period) {
                                ChartPeriod.WEEK -> stringResource(R.string.statistics_period_week)
                                ChartPeriod.MONTH -> stringResource(R.string.statistics_period_month)
                                ChartPeriod.HALF_YEAR -> stringResource(R.string.statistics_period_half_year)
                                ChartPeriod.YEAR -> stringResource(R.string.statistics_period_year)
                                ChartPeriod.ALL_TIME -> stringResource(R.string.statistics_period_all)
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            DailyResultChart(
                rows = state.dailyResults,
                selectedTab = selectedTab,
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
        }

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


@Composable
private fun DailyResultChart(
    rows: List<DailyExerciseResultEntity>,
    selectedTab: Int,
    modifier: Modifier = Modifier,
) {
    val datedValues = rows.mapNotNull { row ->
        val value = when (selectedTab) {
            1 -> row.arithmeticBestTimeMs?.toFloat()
            2 -> row.memoryBestCorrectCount?.toFloat()
            3 -> row.countingBestTimeMs?.toFloat()
            4 -> row.stroopBestTimeMs?.toFloat()
            else -> null
        }
        value?.let {
            LocalDate.parse(row.date.toString(), DateTimeFormatter.BASIC_ISO_DATE) to it
        }
    }.sortedBy { it.first }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (datedValues.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Text(stringResource(R.string.statistics_chart_no_data))
            }
        } else {
            val lineColor = MaterialTheme.colorScheme.primary
            val gridColor = MaterialTheme.colorScheme.outlineVariant
            Column(Modifier.fillMaxSize().padding(12.dp)) {
            Canvas(Modifier.weight(1f).fillMaxWidth()) {
                val left = 8.dp.toPx()
                val right = size.width - 8.dp.toPx()
                val top = 8.dp.toPx()
                val bottom = size.height - 8.dp.toPx()
                val minValue = datedValues.minOf { it.second }
                val maxValue = datedValues.maxOf { it.second }
                val valueRange = (maxValue - minValue).coerceAtLeast(1f)
                val firstDate = datedValues.first().first
                val lastDate = datedValues.last().first
                val dayRange = java.time.temporal.ChronoUnit.DAYS.between(firstDate, lastDate).coerceAtLeast(1L)

                repeat(4) { index ->
                    val y = top + (bottom - top) * index / 3f
                    drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
                }

                val points = datedValues.map { (date, value) ->
                    val x = left + (right - left) *
                        (java.time.temporal.ChronoUnit.DAYS.between(firstDate, date).toFloat() / dayRange.toFloat())
                    val y = bottom - (value - minValue) / valueRange * (bottom - top)
                    Triple(date, value, Offset(x, y))
                }

                // Only connect results from consecutive calendar days; missing days remain visible as gaps.
                points.zipWithNext().forEach { (a, b) ->
                    if (java.time.temporal.ChronoUnit.DAYS.between(a.first, b.first) == 1L) {
                        val midX = (a.third.x + b.third.x) / 2f
                        val path = Path().apply {
                            moveTo(a.third.x, a.third.y)
                            quadraticTo(midX, a.third.y, midX, (a.third.y + b.third.y) / 2f)
                            quadraticTo(midX, b.third.y, b.third.x, b.third.y)
                        }
                        drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx()))
                    }
                }
                points.forEach { (_, _, point) ->
                    drawCircle(lineColor, radius = 4.dp.toPx(), center = point)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = datedValues.first().first.format(DateTimeFormatter.ofPattern("d MMM")),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = datedValues.last().first.format(DateTimeFormatter.ofPattern("d MMM")),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            }
        }
    }
}
