package com.jaroapps.dailybrain.feature.profile.presentation

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.core.presentation.FeedbackPreferences
import com.jaroapps.dailybrain.feature.home.presentation.HomeAppearancePreference
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveSelectedProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.SaveProfilePrivacySettingsUseCase
import com.jaroapps.dailybrain.ui.theme.AppThemePreference
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val observeSelectedProfile: ObserveSelectedProfileUseCase = koinInject()
    val savePrivacySettings: SaveProfilePrivacySettingsUseCase = koinInject()
    val profile by observeSelectedProfile().collectAsStateWithLifecycle(initialValue = null)
    val coroutineScope = rememberCoroutineScope()
    val dynamicBackgroundEnabled = HomeAppearancePreference.dynamicBackgroundEnabled

    var isSavingPrivacy by remember { mutableStateOf(false) }
    var privacySaveError by remember { mutableStateOf<String?>(null) }

    fun persistPrivacySettings(profileInfoVisible: Boolean, feedUpdatesEnabled: Boolean) {
        val selectedProfile = profile ?: return
        if (isSavingPrivacy) return
        coroutineScope.launch {
            isSavingPrivacy = true
            privacySaveError = null
            try {
                savePrivacySettings(
                    localAccountId = selectedProfile.localAccountId,
                    profileInfoVisible = profileInfoVisible,
                    feedUpdatesEnabled = feedUpdatesEnabled,
                )
            } catch (_: Exception) {
                privacySaveError = "Не удалось сохранить настройки приватности. Проверьте подключение к интернету."
            } finally {
                isSavingPrivacy = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Параметры") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Внешний вид", style = MaterialTheme.typography.titleMedium)
            SettingToggle(
                label = stringResource(R.string.preference_dark_theme),
                checked = AppThemePreference.isDarkTheme,
                onCheckedChange = { enabled -> AppThemePreference.setDarkTheme(context, enabled) },
            )
            SettingToggle(
                label = "Динамический фон главного экрана",
                checked = dynamicBackgroundEnabled,
                onCheckedChange = { enabled -> HomeAppearancePreference.setDynamicBackgroundEnabled(context, enabled) },
            )

            Spacer(Modifier.height(4.dp))
            Divider()
            Text("Обратная связь", style = MaterialTheme.typography.titleMedium)
            SettingToggle(
                label = "Звук",
                checked = FeedbackPreferences.soundEnabled,
                onCheckedChange = { FeedbackPreferences.setSoundEnabled(context, it) },
            )
            SettingToggle(
                label = "Вибрация",
                checked = FeedbackPreferences.vibrationEnabled,
                onCheckedChange = { FeedbackPreferences.setVibrationEnabled(context, it) },
            )

            Spacer(Modifier.height(4.dp))
            Divider()
            Text("Управление", style = MaterialTheme.typography.titleMedium)
            HandednessDropdown(
                isLeftHanded = FeedbackPreferences.isLeftHanded,
                onSelectionChanged = { FeedbackPreferences.setLeftHanded(context, it) },
            )
            Text(
                "Влияет на интерфейс в некоторых упражнениях.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))
            Divider()
            Text("Конфиденциальность", style = MaterialTheme.typography.titleMedium)
            SettingToggle(
                label = "Показывать информацию профиля",
                checked = profile?.profileInfoVisible ?: true,
                enabled = profile != null && !isSavingPrivacy,
                onCheckedChange = { enabled ->
                    persistPrivacySettings(
                        profileInfoVisible = enabled,
                        feedUpdatesEnabled = profile?.feedUpdatesEnabled ?: true,
                    )
                },
            )
            SettingToggle(
                label = "Показывать ваши обновления в ленте подписчиков",
                checked = profile?.feedUpdatesEnabled ?: true,
                enabled = profile != null && !isSavingPrivacy,
                onCheckedChange = { enabled ->
                    persistPrivacySettings(
                        profileInfoVisible = profile?.profileInfoVisible ?: true,
                        feedUpdatesEnabled = enabled,
                    )
                },
            )
            privacySaveError?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.preference_appearance_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HandednessDropdown(
    isLeftHanded: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "Ведущая рука",
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(if (isLeftHanded) "Левша" else "Правша")
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Правша") },
                    onClick = {
                        expanded = false
                        onSelectionChanged(false)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Левша") },
                    onClick = {
                        expanded = false
                        onSelectionChanged(true)
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colorResource(R.color.color_on_accent),
                checkedTrackColor = colorResource(R.color.color_button_green),
                checkedBorderColor = colorResource(R.color.color_button_green),
            ),
        )
    }
}
