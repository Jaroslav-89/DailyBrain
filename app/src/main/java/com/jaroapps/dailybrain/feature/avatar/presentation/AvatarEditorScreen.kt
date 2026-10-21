package com.jaroapps.dailybrain.feature.avatar.presentation

import androidx.compose.foundation.horizontalScroll
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
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AvatarPreview(avatar, Modifier.fillMaxWidth().padding(horizontal = 36.dp))

            OptionRow("Тип лица", avatar.faceTypeId, listOf(1 to "Женский", 2 to "Мужской")) {
                viewModel.updateConfiguration(avatar.copy(faceTypeId = it))
            }
            OptionRow("Тон кожи", avatar.skinToneId, (1..5).map { it to "Тон $it" }) {
                viewModel.updateConfiguration(avatar.copy(skinToneId = it))
            }
            OptionRow("Причёска", avatar.hairStyleId, listOf(1 to "Короткая", 2 to "Средняя", 3 to "Длинная", 4 to "Без волос")) {
                viewModel.updateConfiguration(avatar.copy(hairStyleId = it))
            }
            OptionRow("Цвет волос", avatar.hairColorId, listOf(1 to "Чёрные", 2 to "Каштановые", 3 to "Русые", 4 to "Блонд", 5 to "Рыжие")) {
                viewModel.updateConfiguration(avatar.copy(hairColorId = it))
            }
            OptionRow("Форма глаз", avatar.eyeStyleId, listOf(2 to "Большие", 1 to "Средние", 3 to "Азиатские")) {
                viewModel.updateConfiguration(avatar.copy(eyeStyleId = it))
            }
            OptionRow("Цвет глаз", avatar.eyeColorId, listOf(1 to "Карие", 2 to "Голубые", 3 to "Зелёные")) {
                viewModel.updateConfiguration(avatar.copy(eyeColorId = it))
            }
            OptionRow("Улыбка", avatar.mouthStyleId, listOf(1 to "Улыбка", 2 to "Спокойная")) {
                viewModel.updateConfiguration(avatar.copy(mouthStyleId = it))
            }
            OptionRow("Верхняя одежда", avatar.clothingStyleId, listOf(1 to "Футболка", 2 to "Поло", 3 to "Свитер с высоким воротником", 4 to "Платье")) {
                viewModel.updateConfiguration(avatar.copy(clothingStyleId = it))
            }
            OptionRow("Цвет одежды", avatar.clothingColorId, listOf(
                1 to "Чёрный", 2 to "Белый", 3 to "Красный", 4 to "Зелёный", 5 to "Жёлтый",
                6 to "Синий", 7 to "Голубой", 8 to "Розовый", 9 to "Песочный",
                10 to "Салатный", 11 to "Фиолетовый",
            )) {
                viewModel.updateConfiguration(avatar.copy(clothingColorId = it))
            }
            OptionRow("Очки", avatar.glassesId, listOf(
                0 to "Без очков", 1 to "Большая чёрная оправа",
                2 to "Средняя чёрная оправа", 3 to "Солнцезащитные",
            )) {
                viewModel.updateConfiguration(avatar.copy(glassesId = it))
            }
            OptionRow("Веснушки", avatar.frecklesId, listOf(0 to "Нет", 1 to "Мало", 2 to "Много")) {
                viewModel.updateConfiguration(avatar.copy(frecklesId = it))
            }
            OptionRow("Дополнительный аксессуар", avatar.accessoryId, listOf(0 to "Нет", 1 to "Очки", 2 to "Повязка")) {
                viewModel.updateConfiguration(avatar.copy(accessoryId = it))
            }
            OptionRow("Цвет фона", avatar.backgroundId, listOf(
                1 to "Голубой", 2 to "Песочный", 3 to "Мятный", 4 to "Сиреневый",
                5 to "Розовый", 6 to "Небесный", 7 to "Бежевый", 8 to "Салатный",
                9 to "Фиолетовый", 10 to "Бирюзовый", 11 to "Жёлтый", 12 to "Персиковый",
            )) {
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
            ) { Text(if (state.isSaving) "Сохранение…" else "Сохранить") }
            OutlinedButton(
                onClick = viewModel::reset,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Сбросить аватар") }
        }
    }
}

@Composable
private fun OptionRow(
    title: String,
    selected: Int,
    options: List<Pair<Int, String>>,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { (id, label) ->
                if (selected == id) {
                    Button(onClick = { onSelected(id) }) { Text(label, maxLines = 1) }
                } else {
                    OutlinedButton(onClick = { onSelected(id) }) { Text(label, maxLines = 1) }
                }
            }
        }
    }
}
