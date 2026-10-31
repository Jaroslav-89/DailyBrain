package com.jaroapps.dailybrain.feature.history.presentation

import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaroapps.dailybrain.R
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TrainingDayColor = Color(0xFFFF9800)
private val MissedDayColor = Color(0xFF2196F3)
private val TodayWithoutTrainingColor = Color(0xFF9E9E9E)

@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onStartExercise: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_close),
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = "Ваша серия",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { /* Sharing will be implemented in a later iteration. */ },
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Поделиться прогрессом",
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.currentStreakDays.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrainingDayColor,
                )
                Text(
                    text = "дней подряд",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        if (!state.hasTrainingToday) {
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Не прерывайте серию",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Сегодня ещё нет завершённых упражнений. До конца дня осталось примерно ${state.hoursRemainingToday} ч.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = onStartExercise,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Начать упражнение")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = viewModel::previousMonth) { Text("‹") }
            Text(
                text = state.displayedMonth.format(
                    DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()),
                ).replaceFirstChar { it.uppercase(Locale.getDefault()) },
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = viewModel::nextMonth) { Text("›") }
        }

        Spacer(Modifier.height(8.dp))

        val weekdays = listOf(
            R.string.calendar_weekday_monday,
            R.string.calendar_weekday_tuesday,
            R.string.calendar_weekday_wednesday,
            R.string.calendar_weekday_thursday,
            R.string.calendar_weekday_friday,
            R.string.calendar_weekday_saturday,
            R.string.calendar_weekday_sunday,
        ).map { stringResource(it) }

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val month = state.displayedMonth
        val leadingEmptyCells = month.atDay(1).dayOfWeek.value - 1
        val cellCount = ((leadingEmptyCells + month.lengthOfMonth() + 6) / 7) * 7
        val dates: List<LocalDate?> = (0 until cellCount).map { index ->
            val dayNumber = index - leadingEmptyCells + 1
            if (dayNumber in 1..month.lengthOfMonth()) month.atDay(dayNumber) else null
        }

        dates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .then(if (date != null) Modifier.clickable { onDateSelected(date) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            val ringColor = when {
                                date in state.activeDates -> TrainingDayColor
                                date == state.today -> TodayWithoutTrainingColor
                                state.firstTrainingDate != null &&
                                    date >= state.firstTrainingDate &&
                                    date < state.today -> MissedDayColor
                                else -> null
                            }
                            Box(
                                modifier = Modifier.size(38.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (ringColor != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(1.5.dp, ringColor, CircleShape),
                                    )
                                }
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalendarLegend(TrainingDayColor, "Тренировка выполнена")
            CalendarLegend(MissedDayColor, "Пропуск")
            CalendarLegend(TodayWithoutTrainingColor, "Сегодня")
        }
    }
}

@Composable
private fun CalendarLegend(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(8.dp).border(2.dp, color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
