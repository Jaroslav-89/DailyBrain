package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.ui.theme.AppThemePreference
import com.jaroapps.dailybrain.feature.home.presentation.HomeAppearancePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(onBack: () -> Unit) {
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
        val context = LocalContext.current
        val dynamicBackgroundEnabled = HomeAppearancePreference.dynamicBackgroundEnabled

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
                enabled = true,
                onCheckedChange = { enabled ->
                    AppThemePreference.setDarkTheme(context, enabled)
                },
            )
            SettingToggle(
                label = "Динамический фон главного экрана",
                checked = dynamicBackgroundEnabled,
                enabled = true,
                onCheckedChange = { enabled ->
                    HomeAppearancePreference.setDynamicBackgroundEnabled(context, enabled)
                },
            )
            Spacer(Modifier.height(4.dp))
            Divider()
            Text("Уведомления", style = MaterialTheme.typography.titleMedium)
            SettingToggle("Уведомления", enabled = false)
            SettingToggle("Звук", enabled = false)
            SettingToggle("Вибрация", enabled = false)
            Spacer(Modifier.height(4.dp))
            Divider()
            Text("Конфиденциальность", style = MaterialTheme.typography.titleMedium)
            SettingToggle("Информация профиля", enabled = false)
            SettingToggle("Просмотр ленты", enabled = false)
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
private fun SettingToggle(
    label: String,
    checked: Boolean = false,
    enabled: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    androidx.compose.foundation.layout.Row(
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
        )
    }
}
