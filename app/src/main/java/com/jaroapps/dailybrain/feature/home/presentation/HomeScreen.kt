package com.jaroapps.dailybrain.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import org.koin.androidx.compose.koinViewModel
import java.time.format.TextStyle
import java.util.Locale

private val BrainOrange = Color(0xFFFF9800)
private val MissedBlue = Color(0xFF2196F3)
private val UnavailableGray = Color(0xFFBDBDBD)

@Composable
fun HomeScreen(
    onOpenCalendar: () -> Unit,
    onStartTraining: () -> Unit,
    onStartMemoryTraining: () -> Unit,
    onStartStroopTest: () -> Unit,
    onStartCountingTraining: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val weekdayNames = state.currentWeekDates.map {
        it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            .replaceFirstChar { char -> char.uppercase(Locale.getDefault()) }
    }
    val dayButtonsDescription = state.currentWeekDates.zip(weekdayNames)

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable(onClick = onOpenCalendar)
                .semantics { contentDescription = "Серия тренировочных дней: ${state.currentStreakDays}. Открыть календарь" },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.brain_base),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                )
                Column {
                    Text(
                        text = "${state.currentStreakDays}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("дней подряд", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val smallSize = 56.dp
            val largeSize = 88.dp
            fun x(fraction: Float, size: Dp) = (maxWidth - size) * fraction
            fun y(fraction: Float, size: Dp) = (maxHeight - size) * fraction

            // Три дополнительных упражнения по краям дуги.
            RoundExerciseButton(
                label = "M",
                description = "Тренировка памяти",
                size = largeSize,
                x = x(0.02f, largeSize),
                y = y(0.08f, largeSize),
                completed = state.hasCompletedExerciseThisWeek(ExerciseType.MEMORY_WORDS),
                onClick = onStartMemoryTraining,
            )
            RoundExerciseButton(
                label = "STROOP",
                description = "Тест Струпа",
                size = largeSize,
                x = x(0.72f, largeSize),
                y = y(0.43f, largeSize),
                completed = state.hasCompletedExerciseThisWeek(ExerciseType.STROOP_TEST),
                onClick = onStartStroopTest,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFFF9A825), Color(0xFF43A047))
                        .forEach { color ->
                            Box(
                                Modifier
                                    .size(width = 7.dp, height = 28.dp)
                                    .clip(CircleShape)
                                    .background(color),
                            )
                        }
                }
            }
            RoundExerciseButton(
                label = "0..
120",
                description = "Устный счёт",
                size = largeSize,
                x = x(0.04f, largeSize),
                y = y(0.80f, largeSize),
                completed = state.hasCompletedExerciseThisWeek(ExerciseType.COUNTING_TRAINING),
                onClick = onStartCountingTraining,
            )

            val positions = listOf(
                0.78f to 0.02f,
                0.63f to 0.16f,
                0.49f to 0.30f,
                0.36f to 0.44f,
                0.39f to 0.59f,
                0.54f to 0.73f,
                0.69f to 0.86f,
            )
            state.currentWeekDates.forEachIndexed { index, date ->
                val status = state.arithmeticDayStatus(date)
                val color = when (status) {
                    ArithmeticDayStatus.COMPLETED -> BrainOrange
                    ArithmeticDayStatus.MISSED -> MissedBlue
                    ArithmeticDayStatus.AVAILABLE_TODAY,
                    ArithmeticDayStatus.FUTURE -> UnavailableGray
                }
                val enabled = status == ArithmeticDayStatus.AVAILABLE_TODAY
                val today = date == state.today
                val dateLabel = dayButtonsDescription[index].second
                Box(
                    modifier = Modifier
                        .offset(
                            x = x(positions[index].first, smallSize),
                            y = y(positions[index].second, smallSize),
                        )
                        .size(smallSize)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (today) Modifier.border(2.dp, color, CircleShape)
                            else Modifier
                        )
                        .clickable(enabled = enabled, onClick = onStartTraining)
                        .semantics {
                            contentDescription = when (status) {
                                ArithmeticDayStatus.FUTURE -> "$dateLabel: ещё не доступно"
                                ArithmeticDayStatus.AVAILABLE_TODAY -> "$dateLabel: начать арифметику"
                                ArithmeticDayStatus.COMPLETED -> "$dateLabel: тренировка выполнена"
                                ArithmeticDayStatus.MISSED -> "$dateLabel: пропущенный день"
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
                        Icon(Icons.Filled.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundExerciseButton(
    label: String,
    description: String,
    size: Dp,
    x: Dp,
    y: Dp,
    completed: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit = {
        Text(
            text = label,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    },
) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(size)
            .clip(CircleShape)
            .background(if (completed) BrainOrange else UnavailableGray)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
