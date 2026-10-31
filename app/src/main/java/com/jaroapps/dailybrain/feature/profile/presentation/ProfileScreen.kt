package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarPreview
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarViewModel
import com.jaroapps.dailybrain.feature.profile.domain.model.UserGender
import com.jaroapps.dailybrain.feature.profile.data.auth.GoogleSignInClient
import com.jaroapps.dailybrain.feature.profile.data.auth.GoogleSignInConfigurationException
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.BestExerciseResult
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.compose.koinInject
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    onEditAvatar: () -> Unit,
    onOpenFriends: () -> Unit,
    onActiveProfileChanged: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val avatarViewModel: AvatarViewModel = koinViewModel()
    val avatarState by avatarViewModel.uiState.collectAsStateWithLifecycle()
    val googleSignInClient: GoogleSignInClient = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val profile = state.profile
    val stats = state.statistics
    var showAddFriendsDialog by remember { mutableStateOf(false) }
    var showMyQrDialog by remember { mutableStateOf(false) }
    var isGoogleSignInInProgress by remember { mutableStateOf(false) }
    var googleSignInError by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now()
    val trainingPeriod = stats.firstTrainingDate?.let { Period.between(it, today) }
    val accountPeriod = profile?.let {
        val createdDate = Instant.ofEpochMilli(it.createdAtEpochMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        Period.between(createdDate, today).takeIf { elapsed -> !elapsed.isNegative }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Настройки профиля",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (profile?.firebaseUid != null) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Google аккаунт подтверждён",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Text(
                                    text = profile?.let { profileNameWithAge(it, today) }
                                        ?: stringResource(R.string.profile_loading),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            profile?.let { user ->
                                user.googleEmail?.takeIf(String::isNotBlank)?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                user.country?.takeIf(String::isNotBlank)?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium)
                                }
                                user.city?.takeIf(String::isNotBlank)?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium)
                                }
                                user.gender?.let { gender ->
                                    val genderText = when (gender) {
                                        UserGender.FEMALE -> stringResource(R.string.profile_gender_female)
                                        UserGender.MALE -> stringResource(R.string.profile_gender_male)
                                    }
                                    Text(
                                        stringResource(R.string.profile_gender_display, genderText),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
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

            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .clickable(onClick = onOpenFriends),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Подписок", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Подписчиков", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { showAddFriendsDialog = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить друзей")
                }
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    IconButton(
                        onClick = { showMyQrDialog = true },
                        modifier = Modifier.semantics { contentDescription = "Мой QR-код" },
                    ) {
                        Icon(Icons.Filled.QrCode2, contentDescription = "Мой QR-код")
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ProfileStatCard(
                    stringResource(R.string.profile_current_streak),
                    stats.currentStreakDays.toString(),
                    Modifier.weight(1f),
                )
                ProfileStatCard(
                    stringResource(R.string.profile_best_streak),
                    stats.bestStreakDays.toString(),
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ProfileStatCard(
                    stringResource(R.string.profile_total_exercises),
                    stats.totalExerciseCount.toString(),
                    Modifier.weight(1f),
                )
                ProfileStatCard(
                    stringResource(R.string.profile_week_exercises),
                    stats.lastSevenDaysExerciseCount.toString(),
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            ProfileStatCard(
                stringResource(R.string.profile_total_duration),
                formatDuration(stats.totalDurationMs),
                Modifier.fillMaxWidth(),
            )
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
                ProfileStatCard(
                    stringResource(R.string.profile_training_period),
                    durationText,
                    Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                stringResource(R.string.profile_best_results),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            BestResultCard(
                stringResource(R.string.exercise_arithmetic),
                stats.bestExerciseStats.firstOrNull { it.exerciseType == ExerciseType.ARITHMETIC.storageValue },
                BestKind.DURATION,
            )
            Spacer(Modifier.height(8.dp))
            BestResultCard(
                stringResource(R.string.exercise_memory),
                stats.bestExerciseStats.firstOrNull { it.exerciseType == ExerciseType.MEMORY_WORDS.storageValue },
                BestKind.MEMORY,
            )
            Spacer(Modifier.height(8.dp))
            BestResultCard(
                stringResource(R.string.exercise_counting),
                stats.bestExerciseStats.firstOrNull { it.exerciseType == ExerciseType.COUNTING_TRAINING.storageValue },
                BestKind.DURATION,
            )
            Spacer(Modifier.height(8.dp))
            BestResultCard(
                stringResource(R.string.exercise_stroop),
                stats.bestExerciseStats.firstOrNull { it.exerciseType == ExerciseType.STROOP_TEST.storageValue },
                BestKind.DURATION,
            )

            if (state.profiles.size > 1) {
                Spacer(Modifier.height(22.dp))
                Text("Локальные профили", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
            }
            OutlinedButton(
                onClick = { viewModel.createProfile(onActiveProfileChanged) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Добавить локальный профиль")
            }
            if (state.saveError) {
                Spacer(Modifier.height(8.dp))
                Text("Не удалось выполнить действие с профилем.", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAddFriendsDialog) {
        AddFriendsDialog(
            googleLinked = profile?.googleSubject != null,
            googleEmail = profile?.googleEmail,
            isGoogleSignInInProgress = isGoogleSignInInProgress || state.isGoogleLinking,
            googleLinkError = state.googleLinkError,
            googleSignInError = googleSignInError,
            onContinueWithGoogle = {
                coroutineScope.launch {
                    isGoogleSignInInProgress = true
                    googleSignInError = null
                    viewModel.clearGoogleLinkError()
                    try {
                        val signInResult = googleSignInClient.signIn()
                        viewModel.linkGoogleAccount(signInResult)
                    } catch (error: Exception) {
                        googleSignInError = if (error is GoogleSignInConfigurationException) {
                            error.message
                        } else {
                            "Не удалось выполнить вход с Google. Попробуйте ещё раз."
                        }
                    } finally {
                        isGoogleSignInInProgress = false
                    }
                }
            },
            onDismiss = {
                showAddFriendsDialog = false
                googleSignInError = null
                viewModel.clearGoogleLinkError()
            },
        )
    }
    if (showMyQrDialog) {
        MyQrCodeDialog(profile = profile, onDismiss = { showMyQrDialog = false })
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
