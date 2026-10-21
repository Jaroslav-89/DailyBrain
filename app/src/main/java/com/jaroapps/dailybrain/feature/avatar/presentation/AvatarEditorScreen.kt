package com.jaroapps.dailybrain.feature.avatar.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration
import org.koin.androidx.compose.koinViewModel

private val editorTabs = AvatarEditorTab.values().toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarEditorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AvatarViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val avatar = state.configuration
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

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
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AvatarPreview(avatar, Modifier.fillMaxWidth().padding(horizontal = 72.dp, vertical = 4.dp))
            ScrollableTabRow(
                selectedTabIndex = selectedTab.coerceIn(editorTabs.indices),
                edgePadding = 8.dp,
            ) {
                editorTabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { AvatarTabIcon(tab, selected = selectedTab == index) },
                        text = null,
                        modifier = Modifier.semantics {
                            contentDescription = tab.label
                            selected = selectedTab == index
                        },
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (editorTabs[selectedTab.coerceIn(editorTabs.indices)]) {
                    AvatarEditorTab.SKIN -> OptionRow("Цвет кожи", avatar.skinToneId, listOf(
                        1 to "Очень светлый", 2 to "Светлый", 3 to "Средний", 4 to "Смуглый", 5 to "Тёмный",
                    )) { viewModel.updateConfiguration(avatar.copy(skinToneId = it)) }

                    AvatarEditorTab.FACE -> OptionRow("Тип лица", avatar.faceTypeId, listOf(
                        1 to "Овальное", 2 to "Угловатое", 3 to "Круглое", 4 to "Узкое", 5 to "Широкое",
                    )) { viewModel.updateConfiguration(avatar.copy(faceTypeId = it)) }

                    AvatarEditorTab.BODY -> OptionRow("Тип тела", avatar.bodyTypeId, listOf(
                        1 to "Стройное", 2 to "Спортивное", 3 to "Среднее", 4 to "Крупное", 5 to "Широкое",
                    )) { viewModel.updateConfiguration(avatar.copy(bodyTypeId = it)) }

                    AvatarEditorTab.EYES -> {
                        OptionRow("Форма глаз", avatar.eyeStyleId, listOf(
                            1 to "Средние", 2 to "Большие", 3 to "Миндалевидные", 4 to "Прищуренные", 5 to "Круглые",
                        )) { viewModel.updateConfiguration(avatar.copy(eyeStyleId = it)) }
                        OptionRow("Цвет радужки", avatar.eyeColorId, listOf(
                            1 to "Карий", 2 to "Голубой", 3 to "Зелёный", 4 to "Серый", 5 to "Ореховый",
                        )) { viewModel.updateConfiguration(avatar.copy(eyeColorId = it)) }
                    }

                    AvatarEditorTab.HAIRSTYLE -> OptionRow("Тип причёски", avatar.hairStyleId, listOf(
                        1 to "Прямые", 2 to "Волнистые", 3 to "Чёлка", 4 to "Кудри", 5 to "Зачёс",
                    )) { viewModel.updateConfiguration(avatar.copy(hairStyleId = it)) }

                    AvatarEditorTab.HAIR_LENGTH -> OptionRow("Длина волос", avatar.hairLengthId, listOf(
                        1 to "Очень короткие", 2 to "Короткие", 3 to "До плеч", 4 to "Длинные", 5 to "Очень длинные",
                    )) { viewModel.updateConfiguration(avatar.copy(hairLengthId = it)) }

                    AvatarEditorTab.HAIR_COLOR -> OptionRow("Цвет волос", avatar.hairColorId, listOf(
                        1 to "Чёрный", 2 to "Каштановый", 3 to "Русый", 4 to "Блонд", 5 to "Рыжий",
                    )) { viewModel.updateConfiguration(avatar.copy(hairColorId = it)) }

                    AvatarEditorTab.CLOTHING -> OptionRow("Тип одежды", avatar.clothingStyleId, listOf(
                        1 to "Футболка", 2 to "Поло", 3 to "Свитер", 4 to "Платье", 5 to "Худи",
                    )) { viewModel.updateConfiguration(avatar.copy(clothingStyleId = it)) }

                    AvatarEditorTab.CLOTHING_COLOR -> OptionRow("Цвет одежды", avatar.clothingColorId, listOf(
                        1 to "Чёрный", 2 to "Белый", 3 to "Красный", 4 to "Зелёный", 5 to "Жёлтый",
                        6 to "Синий", 7 to "Голубой", 8 to "Розовый", 9 to "Песочный", 10 to "Салатный", 11 to "Фиолетовый",
                    )) { viewModel.updateConfiguration(avatar.copy(clothingColorId = it)) }

                    AvatarEditorTab.GLASSES -> OptionRow("Очки", avatar.glassesId, listOf(
                        0 to "Без очков",
                        1 to "Крупная оправа",
                        2 to "Тонкая оправа",
                        3 to "Солнцезащитные",
                        4 to "Круглые",
                        5 to "Синяя оправа",
                    )) { viewModel.updateConfiguration(avatar.copy(glassesId = it)) }

                    AvatarEditorTab.FRECKLES -> OptionRow("Веснушки", avatar.frecklesId, listOf(
                        0 to "Нет",
                        1 to "Немного",
                        2 to "Много",
                        3 to "На щеках",
                        4 to "На носу",
                    )) { viewModel.updateConfiguration(avatar.copy(frecklesId = it)) }

                    AvatarEditorTab.FACIAL_HAIR -> OptionRow("Борода и усы", avatar.facialHairId, listOf(
                        0 to "Без волос",
                        1 to "Усы",
                        2 to "Борода",
                        3 to "Щетина",
                        4 to "Козлиная бородка",
                    )) { viewModel.updateConfiguration(avatar.copy(facialHairId = it)) }

                    AvatarEditorTab.BACKGROUND -> BackgroundOptions(
                        selected = avatar.backgroundId,
                        onSelected = { viewModel.updateConfiguration(avatar.copy(backgroundId = it)) },
                    )

                    AvatarEditorTab.OTHER -> {
                        OptionRow("Выражение лица", avatar.mouthStyleId, listOf(1 to "Улыбка", 2 to "Спокойное")) {
                            viewModel.updateConfiguration(avatar.copy(mouthStyleId = it))
                        }
                        OptionRow("Аксессуар", avatar.accessoryId, listOf(0 to "Нет", 2 to "Повязка")) {
                            viewModel.updateConfiguration(avatar.copy(accessoryId = it))
                        }
                    }
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
}

@Composable
private fun BackgroundOptions(
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Градиент фона", style = MaterialTheme.typography.titleSmall)
        Text("Переход цвета из левого верхнего угла в правый нижний", style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBackgroundPalette.gradients.take(10).forEachIndexed { index, colors ->
                val id = index + 1
                val isSelected = selected == id
                val shape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .size(width = 58.dp, height = 48.dp)
                        .clip(shape)
                        .background(Brush.linearGradient(colors = colors.map { Color(it) }), shape)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = shape,
                        )
                        .clickable { onSelected(id) }
                        .semantics {
                            contentDescription = "Градиент фона $id"
                            selected = isSelected
                        },
                )
            }
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
