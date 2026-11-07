package com.jaroapps.dailybrain.feature.publicprofile.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.feature.avatar.data.AvatarJsonCodec
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarPreview
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicBestExerciseResult
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectibleKind
import com.jaroapps.dailybrain.feature.profile.presentation.ProfileCollectibleSection
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicUserProfileScreen(
    profileFirebaseUid: String,
    onBack: () -> Unit,
    onOpenConnections: (String, String) -> Unit,
    onOpenCollectibles: (String, ProfileCollectibleKind) -> Unit,
    onOpenCollectible: (String, ProfileCollectibleKind, String) -> Unit,
    viewModel: PublicUserProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showBlockConfirmation by remember { mutableStateOf(false) }
    LaunchedEffect(profileFirebaseUid) { viewModel.load(profileFirebaseUid) }

    val user = state.user
    val stats = user?.statistics
    val today = LocalDate.now()
    val trainingPeriod = stats?.firstTrainingDate
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.let { firstDate -> Period.between(firstDate, today).plusDays(1).takeIf { !it.isNegative } }
    val memberPeriod = user?.memberSinceEpochMs?.let { epoch ->
        val date = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate()
        Period.between(date, today).plusDays(1).takeIf { !it.isNegative }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль пользователя") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 0.dp),
        ) {
            when {
                state.isLoading && user == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                state.error != null && user == null -> {
                    Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
                user != null -> {
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
                                        configuration = AvatarJsonCodec.decode(user.avatarConfigurationJson),
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = user.displayName?.takeIf(String::isNotBlank)
                                            ?: "Пользователь DailyBrain",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (user.username.isNotBlank()) {
                                        Text("@${user.username}", color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (!user.isRestricted) {
                                        user.country?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                        user.city?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                        user.gender?.let { gender ->
                                            val label = when (gender) {
                                                "FEMALE" -> "Женский"
                                                "MALE" -> "Мужской"
                                                else -> null
                                            }
                                            label?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                        }
                                    }
                                }
                            }
                            if (!user.isRestricted) {
                                memberPeriod?.let { period ->
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "В DailyBrain: ${formatPublicPeriod(period)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    if (user.isRestricted) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Доступ ограничен",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clip(CircleShape).clickable {
                                    onOpenConnections(user.firebaseUid, PublicUserConnectionsViewModel.FOLLOWING)
                                }.padding(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Text(state.followingUsers.size.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Подписок", style = MaterialTheme.typography.bodyMedium)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clip(CircleShape).clickable {
                                    onOpenConnections(user.firebaseUid, PublicUserConnectionsViewModel.FOLLOWERS)
                                }.padding(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Text(state.followerUsers.size.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Подписчиков", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    if (!state.isSelf) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = viewModel::toggleFollow,
                            enabled = !state.isUpdatingFollow,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isFollowing) colorResource(R.color.color_danger) else colorResource(R.color.color_success),
                                contentColor = colorResource(R.color.color_on_accent),
                            ),
                        ) {
                            Text(
                                when {
                                    state.isUpdatingFollow -> "Обновляем подписку..."
                                    state.isFollowing -> "Отписаться"
                                    else -> "Подписаться"
                                },
                            )
                        }
                    }
                    ProfileCollectibleSection(
                        title = "Подарки",
                        ownerUid = user.firebaseUid,
                        items = state.collectibles.filter { it.kind == ProfileCollectibleKind.GIFT },
                        kind = ProfileCollectibleKind.GIFT,
                        onOpenCollection = onOpenCollectibles,
                        onOpenItem = onOpenCollectible,
                    )
                    if (state.collectibles.any { it.kind == ProfileCollectibleKind.GIFT }) {
                        Spacer(Modifier.height(14.dp))
                    }
                    ProfileCollectibleSection(
                        title = "Бейджики",
                        ownerUid = user.firebaseUid,
                        items = state.collectibles.filter { it.kind == ProfileCollectibleKind.BADGE },
                        kind = ProfileCollectibleKind.BADGE,
                        onOpenCollection = onOpenCollectibles,
                        onOpenItem = onOpenCollectible,
                    )
                    if (stats != null) {
                        Spacer(Modifier.height(22.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            PublicProfileStatCard("Текущая серия", stats.currentStreakDays.toString(), Modifier.weight(1f))
                            PublicProfileStatCard("Лучшая серия", stats.bestStreakDays.toString(), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            PublicProfileStatCard("Всего упражнений", stats.totalExerciseCount.toString(), Modifier.weight(1f))
                            PublicProfileStatCard("За 7 дней", stats.lastSevenDaysExerciseCount.toString(), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        PublicProfileStatCard("Общее время тренировок", formatPublicDuration(stats.totalDurationMs), Modifier.fillMaxWidth())
                        trainingPeriod?.let { period ->
                            Spacer(Modifier.height(10.dp))
                            PublicProfileStatCard("Период тренировок", formatPublicPeriod(period), Modifier.fillMaxWidth())
                        }

                        Spacer(Modifier.height(22.dp))
                        Text("Лучшие результаты", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        PublicBestResultCard("Арифметика", stats.bestExerciseStats.firstOrNull { it.exerciseType == ExerciseType.ARITHMETIC.storageValue }, PublicBestKind.DURATION)
                        Spacer(Modifier.height(8.dp))
                        PublicBestResultCard("Тренировка памяти", stats.bestExerciseStats.firstOrNull { it.exerciseType == ExerciseType.MEMORY_WORDS.storageValue }, PublicBestKind.MEMORY)
                        Spacer(Modifier.height(8.dp))
                        PublicBestResultCard("Счёт", stats.bestExerciseStats.firstOrNull { it.exerciseType == ExerciseType.COUNTING_TRAINING.storageValue }, PublicBestKind.DURATION)
                        Spacer(Modifier.height(8.dp))
                        PublicBestResultCard("Тест Струпа", stats.bestExerciseStats.firstOrNull { it.exerciseType == ExerciseType.STROOP_TEST.storageValue }, PublicBestKind.DURATION)
                    }
                    }

                    state.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    if (!state.isSelf && (!user.isRestricted || state.isBlockedByViewer)) {
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { showBlockConfirmation = true },
                            enabled = !state.isUpdatingBlock,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isBlockedByViewer) colorResource(R.color.color_success) else colorResource(R.color.color_danger),
                                contentColor = colorResource(R.color.color_on_accent),
                            ),
                        ) {
                            Text(
                                when {
                                    state.isUpdatingBlock -> "Обновляем блокировку..."
                                    state.isBlockedByViewer -> "Разблокировать пользователя"
                                    else -> "Заблокировать пользователя"
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBlockConfirmation && user != null) {
        val isUnblocking = state.isBlockedByViewer
        val userLabel = user.displayName?.takeIf(String::isNotBlank)
            ?: user.username.takeIf(String::isNotBlank)?.let { "@$it" }
            ?: "этого пользователя"
        AlertDialog(
            onDismissRequest = { showBlockConfirmation = false },
            title = {
                Text(if (isUnblocking) "Разблокировать пользователя?" else "Заблокировать пользователя?")
            },
            text = {
                Text(
                    if (isUnblocking) {
                        "Пользователь $userLabel снова сможет просматривать ваш полный профиль, если не заблокировал вас в ответ."
                    } else {
                        "Пользователь $userLabel больше не сможет просматривать ваш полный профиль. Взаимные подписки будут удалены."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockConfirmation = false
                        viewModel.toggleBlock()
                    },
                ) {
                    Text(
                        if (isUnblocking) "Разблокировать" else "Заблокировать",
                        color = if (isUnblocking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmation = false }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun PublicProfileStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

private enum class PublicBestKind { DURATION, MEMORY }

@Composable
private fun PublicBestResultCard(title: String, stats: PublicBestExerciseResult?, kind: PublicBestKind) {
    val value = when (kind) {
        PublicBestKind.DURATION -> stats?.bestDurationMs?.let(::formatPublicDuration)
        PublicBestKind.MEMORY -> stats?.bestCorrectCount?.toString()
    }
    PublicProfileStatCard(title, value ?: "Нет результатов", Modifier.fillMaxWidth())
}

private fun formatPublicDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours} ч ${minutes} мин"
        minutes > 0 -> "${minutes} мин ${seconds} сек"
        else -> "${seconds} сек"
    }
}

private fun formatPublicPeriod(period: Period): String = buildList {
    if (period.years > 0) add("${period.years} л.")
    if (period.months > 0) add("${period.months} мес.")
    add("${period.days} дн.")
}.joinToString(" ")

