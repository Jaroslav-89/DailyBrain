package com.jaroapps.dailybrain.feature.history.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.jaroapps.dailybrain.R
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
            Text("‹ " + stringResource(R.string.action_back))
        }

        Text(
            text = date.format(dateFormatter)
                .replaceFirstChar { it.uppercase(Locale.getDefault()) },
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.size(8.dp))

        Text(
            text = if (sessions.isEmpty()) {
                stringResource(R.string.history_no_trainings)
            } else {
                pluralStringResource(
                    R.plurals.history_training_count,
                    sessions.size,
                    sessions.size,
                )
            },
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(Modifier.size(16.dp))

        if (sessions.isEmpty()) {
            Text(
                text = stringResource(R.string.history_empty_day),
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
                            val exerciseName = when (ExerciseType.fromStorage(session.exerciseType)) {
                                ExerciseType.ARITHMETIC -> stringResource(R.string.exercise_arithmetic)
                                ExerciseType.MEMORY_WORDS -> stringResource(R.string.exercise_memory)
                                ExerciseType.COUNTING_TRAINING -> stringResource(R.string.exercise_counting)
                                ExerciseType.STROOP_TEST -> stringResource(R.string.exercise_stroop)
                                null -> session.exerciseType
                            }
                            Text(
                                text = exerciseName,
                                style = MaterialTheme.typography.titleMedium,
                            )

                            Spacer(Modifier.size(8.dp))

                            val finishedTime = Instant
                                .ofEpochMilli(session.finishedAtEpochMs)
                                .atZone(zone)
                                .format(timeFormatter)

                            Text(stringResource(R.string.label_finished_at, finishedTime))

                            Spacer(Modifier.size(4.dp))

                            Text(stringResource(R.string.label_duration, formatDuration(session.durationMs)))

                            Spacer(Modifier.size(4.dp))

                            Text(
                                stringResource(
                                    R.string.history_accuracy,
                                    String.format(Locale.getDefault(), "%.0f", session.accuracyPercent),
                                )
                            )

                            Spacer(Modifier.size(4.dp))

                            Text(
                                stringResource(
                                    R.string.history_correct_answers,
                                    session.correctCount,
                                    session.taskCount,
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return if (minutes > 0) {
        stringResource(R.string.statistics_duration_minutes, minutes, seconds)
    } else {
        stringResource(R.string.statistics_duration_seconds, seconds)
    }
}
