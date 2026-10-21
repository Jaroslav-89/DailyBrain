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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jaroapps.dailybrain.R
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    onOpenProfile: () -> Unit,
    onOpenCalendar: () -> Unit,
    onStartMemoryTraining: () -> Unit,
    onStartStroopTest: () -> Unit,
    onStartCountingTraining: () -> Unit,
    onOpenArticle: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val articleDescription = stringResource(R.string.home_open_article)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                onClick = onOpenArticle,
                modifier = Modifier.semantics {
                    contentDescription = articleDescription
                },
            ) {
                Text(
                    text = "ⓘ",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(Modifier.height(36.dp))

        Text(
            text = if (state.isTrainingCompletedToday) {
                stringResource(R.string.home_training_done_today)
            } else {
                stringResource(R.string.home_not_trained_today)
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
                            Locale.getDefault(),
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
                    text = stringResource(R.string.label_training_count_total),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onOpenProfile,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.home_profile))
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenStatistics,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.home_statistics))
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onStartTraining,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.exercise_arithmetic))
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onStartMemoryTraining,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.exercise_memory))
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onStartStroopTest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.exercise_stroop))
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onStartCountingTraining,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.exercise_counting))
        }
    }
}
