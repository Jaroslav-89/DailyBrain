package com.jaroapps.dailybrain.feature.profile.presentation

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.feature.profile.domain.repository.BestExerciseResult
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.Period

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile
    val stats = state.statistics
    val today = LocalDate.now()
    val period = stats.firstTrainingDate?.let { Period.between(it, today) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.profile_title)) },
            actions = {
                IconButton(onClick = onEditProfile) {
                    Text("⚙", style = MaterialTheme.typography.titleLarge)
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = profile?.effectiveDisplayName ?: stringResource(R.string.profile_loading),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                profile?.let { user ->
                    val location = listOfNotNull(
                        user.city?.takeIf(String::isNotBlank),
                        user.country?.takeIf(String::isNotBlank),
                    ).joinToString(", ")
                    if (location.isNotBlank()) {
                        Text(location, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = stringResource(R.string.profile_alias, user.generatedAlias),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.profile_summary), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileStatCard(stringResource(R.string.profile_current_streak), stats.currentStreakDays.toString(), Modifier.weight(1f))
            ProfileStatCard(stringResource(R.string.profile_best_streak), stats.bestStreakDays.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileStatCard(stringResource(R.string.profile_total_exercises), stats.totalExerciseCount.toString(), Modifier.weight(1f))
            ProfileStatCard(stringResource(R.string.profile_week_exercises), stats.lastSevenDaysExerciseCount.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        ProfileStatCard(stringResource(R.string.profile_total_duration), formatDuration(stats.totalDurationMs), Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        period?.let {
            val durationText = listOfNotNull(
                stringResource(R.string.profile_years, it.years).takeIf { _ -> it.years > 0 },
                stringResource(R.string.profile_months, it.months).takeIf { _ -> it.months > 0 },
                stringResource(R.string.profile_days, it.days),
            ).joinToString(" ")
            ProfileStatCard(stringResource(R.string.profile_training_period), durationText, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(22.dp))
        Text(stringResource(R.string.profile_best_results), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        BestResultCard(stringResource(R.string.exercise_arithmetic), stats.bestExerciseStats.firstOrNull { it.exerciseType == ExerciseType.ARITHMETIC.storageValue }, BestKind.DURATION)
        Spacer(Modifier.height(8.dp))
        BestResultCard(stringResource(R.string.exercise_memory), stats.bestExerciseStats.firstOrNull { it.exerciseType == ExerciseType.MEMORY_WORDS.storageValue }, BestKind.MEMORY)
        Spacer(Modifier.height(8.dp))
        BestResultCard(stringResource(R.string.exercise_counting), stats.bestExerciseStats.firstOrNull { it.exerciseType == ExerciseType.COUNTING_TRAINING.storageValue }, BestKind.DURATION)
        Spacer(Modifier.height(8.dp))
        BestResultCard(stringResource(R.string.exercise_stroop), stats.bestExerciseStats.firstOrNull { it.exerciseType == ExerciseType.STROOP_TEST.storageValue }, BestKind.DURATION)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_home))
        }
    }
}

@Composable
private fun ProfileStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

private enum class BestKind { DURATION, MEMORY }

@Composable
private fun BestResultCard(title: String, stats: BestExerciseResult?, kind: BestKind) {
    val value = when (kind) {
        BestKind.DURATION -> stats?.bestDurationMs?.let(::formatDuration)
        BestKind.MEMORY -> stats?.bestCorrectCount?.toString()
    }
    ProfileStatCard(
        title = title,
        value = value ?: stringResource(R.string.statistics_no_record),
        modifier = Modifier.fillMaxWidth(),
    )
}

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
