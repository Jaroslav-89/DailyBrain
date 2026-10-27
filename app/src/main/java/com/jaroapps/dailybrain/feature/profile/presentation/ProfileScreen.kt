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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarPreview
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarViewModel
import com.jaroapps.dailybrain.feature.profile.domain.model.UserGender
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.Period

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFriends: () -> Unit,
    onActiveProfileChanged: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val avatarViewModel: AvatarViewModel = koinViewModel()
    val avatarState by avatarViewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile
    var showAddFriendsDialog by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.width(82.dp).aspectRatio(3f / 4f),
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
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = profile?.let { profileNameWithAge(it, today) } ?: "Загрузка профиля…",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    profile?.let { user ->
                        user.country?.takeIf(String::isNotBlank)?.let { Text(it) }
                        user.city?.takeIf(String::isNotBlank)?.let { Text(it) }
                        user.gender?.let {
                            val genderLabel = when (it) {
                                UserGender.FEMALE -> "Женский"
                                UserGender.MALE -> "Мужской"
                            }
                            Text(genderLabel, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (user.displayName.isNullOrBlank()) {
                            Text(user.generatedAlias, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (profile?.firebaseUid != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFriends),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Подписки", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Подписчики", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            OutlinedButton(onClick = { showAddFriendsDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить друзей")
            }
        }

        OutlinedButton(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) {
            Text("Профиль")
        }
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Параметры")
        }
        OutlinedButton(onClick = { /* Feedback flow will be added later. */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Обратная связь")
        }
        OutlinedButton(onClick = { /* Authentication is not connected in this local-only branch. */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Выйти из аккаунта")
        }

        if (state.profiles.size > 1) {
            Spacer(Modifier.height(4.dp))
            Text("Локальные профили", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            state.profiles.forEach { localProfile ->
                OutlinedButton(
                    onClick = { viewModel.switchProfile(localProfile.localAccountId, onActiveProfileChanged) },
                    enabled = localProfile.localAccountId != profile?.localAccountId,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val activeMark = if (localProfile.localAccountId == profile?.localAccountId) "✓ " else ""
                    Text(activeMark + localProfile.effectiveDisplayName)
                }
            }
        }
        OutlinedButton(
            onClick = { viewModel.createProfile(onActiveProfileChanged) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Добавить локальный профиль") }

        if (state.saveError) {
            Text("Не удалось выполнить действие с профилем.", color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clickable {
                // Privacy policy page is not part of this UI-only iteration.
            }.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Политика конфиденциальности",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { contentDescription = "Политика конфиденциальности" },
            )
        }
    }

    if (showAddFriendsDialog) {
        AddFriendsDialog(onDismiss = { showAddFriendsDialog = false })
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
