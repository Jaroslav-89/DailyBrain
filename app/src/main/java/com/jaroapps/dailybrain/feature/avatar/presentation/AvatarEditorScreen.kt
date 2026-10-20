package com.jaroapps.dailybrain.feature.avatar.presentation

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarEditorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AvatarViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val avatar = state.configuration

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Мой аватар") },
                navigationIcon = {
                    androidx.compose.material3.TextButton(onClick = onBack) { Text("Назад") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AvatarPreview(avatar, Modifier.fillMaxWidth().padding(horizontal = 36.dp))
            ChoiceRow("Тон кожи", avatar.skinToneId, 5) {
                viewModel.updateConfiguration(avatar.copy(skinToneId = it))
            }
            ChoiceRow("Причёска", avatar.hairStyleId, 3) {
                viewModel.updateConfiguration(avatar.copy(hairStyleId = it))
            }
            ChoiceRow("Цвет волос", avatar.hairColorId, 5) {
                viewModel.updateConfiguration(avatar.copy(hairColorId = it))
            }
            ChoiceRow("Глаза", avatar.eyeStyleId, 2) {
                viewModel.updateConfiguration(avatar.copy(eyeStyleId = it))
            }
            ChoiceRow("Улыбка", avatar.mouthStyleId, 2) {
                viewModel.updateConfiguration(avatar.copy(mouthStyleId = it))
            }
            ChoiceRow("Аксессуар", avatar.accessoryId, 3, firstOption = 0) {
                viewModel.updateConfiguration(avatar.copy(accessoryId = it))
            }
            ChoiceRow("Фон", avatar.backgroundId, 4) {
                viewModel.updateConfiguration(avatar.copy(backgroundId = it))
            }
            if (state.saveError) {
                Text("Не удалось сохранить аватар. Попробуйте ещё раз.", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { viewModel.save(onBack) },
                enabled = !state.isSaving && !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaving) "Сохранение…" else "Сохранить")
            }
            OutlinedButton(
                onClick = viewModel::reset,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сбросить аватар")
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    selected: Int,
    count: Int,
    firstOption: Int = 1,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            (firstOption until firstOption + count).forEach { option ->
                val label = if (title == "Аксессуар" && option == 0) "Нет" else option.toString()
                if (selected == option) {
                    Button(onClick = { onSelected(option) }, modifier = Modifier.weight(1f)) { Text(label) }
                } else {
                    OutlinedButton(onClick = { onSelected(option) }, modifier = Modifier.weight(1f)) { Text(label) }
                }
            }
        }
    }
}
