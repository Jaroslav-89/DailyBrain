package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarPreview
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarViewModel
import com.jaroapps.dailybrain.feature.profile.domain.model.UserGender
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.BestExerciseResult
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onActiveProfileChanged: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val avatarViewModel: AvatarViewModel = koinViewModel()
    val avatarState by avatarViewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile
    val stats = state.statistics
    val today = LocalDate.now()
    val trainingPeriod = stats.firstTrainingDate?.let { Period.between(it, today) }
    val accountPeriod = profile?.let {
        val createdDate = Instant.ofEpochMilli(it.createdAtEpochMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        Period.between(createdDate, today).takeIf { elapsed -> !elapsed.isNegative }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.profile_title)) },
            actions = {
                IconButton(onClick = onEditProfile) {
                    Text("⚙", modifier = Modifier.semantics { contentDescription = "Edit profile" }, style = MaterialTheme.typography.titleLarge)
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Surface(
                        modifier = Modifier.width(84.dp).aspectRatio(3f / 4f),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        AvatarPreview(
                            configuration = avatarState.configuration,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = profile?.let { user -> profileNameWithAge(user, today) }
                                ?: stringResource(R.string.profile_loading),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        profile?.let { user ->
                            user.country?.takeIf(String::isNotBlank)?.let { country ->
                                Text(country, style = MaterialTheme.typography.bodyMedium)
                            }
                            user.city?.takeIf(String::isNotBlank)?.let { city ->
                                Text(city, style = MaterialTheme.typography.bodyMedium)
                            }
                            user.gender?.let { gender ->
                                val genderText = when (gender) {
                                    UserGender.FEMALE -> stringResource(R.string.profile_gender_female)
                                    UserGender.MALE -> stringResource(R.string.profile_gender_male)
                                }
                                Text(stringResource(R.string.profile_gender_display, genderText), style = MaterialTheme.typography.bodyMedium)
                            }
                            if (user.displayName.isNullOrBlank()) {
                                Text(
                                    text = stringResource(R.string.profile_alias, user.generatedAlias),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                accountPeriod?.let { elapsed ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.profile_member_since, formatAccountPeriod(elapsed)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.profile_accounts),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        state.profiles.forEach { localProfile ->
            OutlinedButton(
                onClick = { viewModel.switchProfile(localProfile.localAccountId, onActiveProfileChanged) },
                enabled = localProfile.localAccountId != profile?.localAccountId,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val activeMark = if (localProfile.localAccountId == profile?.localAccountId) "✓ " else ""
                Text(activeMark + localProfile.effectiveDisplayName)
            }
            Spacer(Modifier.height(6.dp))
        }
        OutlinedButton(
            onClick = { viewModel.createProfile(onActiveProfileChanged) },
            enabled = true,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.profile_add_account))
        }
        if (state.saveError) {
            Text(
                text = stringResource(R.string.profile_account_action_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
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
        trainingPeriod?.let { elapsed ->
            val yearsText = stringResource(R.string.profile_years, elapsed.years)
            val monthsText = stringResource(R.string.profile_months, elapsed.months)
            val daysText = stringResource(R.string.profile_days, elapsed.days)
            val durationText = listOfNotNull(
                yearsText.takeIf { elapsed.years > 0 },
                monthsText.takeIf { elapsed.months > 0 },
                daysText,
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
private fun profileNameWithAge(profile: UserProfile, today: LocalDate): String {
    val age = profile.birthDate?.let { birthDate ->
        Period.between(birthDate, today).years.takeIf { !Period.between(birthDate, today).isNegative }
    }
    if (age == null) return profile.effectiveDisplayName
    return profile.effectiveDisplayName + ", " + pluralStringResource(R.plurals.profile_age_years, age, age)
}

@Composable
private fun formatAccountPeriod(period: Period): String {
    val parts = buildList {
        if (period.years > 0) add(pluralStringResource(R.plurals.profile_tenure_years, period.years, period.years))
        if (period.months > 0) add(pluralStringResource(R.plurals.profile_tenure_months, period.months, period.months))
        add(pluralStringResource(R.plurals.profile_tenure_days, period.days, period.days))
    }
    return parts.joinToString(" ")
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
        BestKind.DURATION -> {
            val durationMs = stats?.bestDurationMs
            if (durationMs != null) formatDuration(durationMs) else null
        }
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
