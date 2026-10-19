package com.jaroapps.dailybrain.feature.history.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jaroapps.dailybrain.R
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.action_back))
            }

            Text(
                text = stringResource(R.string.calendar_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Spacer(Modifier.size(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = viewModel::previousMonth) {
                Text("‹")
            }

            Text(
                text = state.displayedMonth.format(
                    DateTimeFormatter.ofPattern(
                        "LLLL yyyy",
                        Locale.getDefault(),
                    )
                ).replaceFirstChar { it.uppercase(Locale("ru")) },
                style = MaterialTheme.typography.titleMedium,
            )

            TextButton(onClick = viewModel::nextMonth) {
                Text("›")
            }
        }

        Spacer(Modifier.size(12.dp))

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

        Spacer(Modifier.size(8.dp))

        val month = state.displayedMonth
        val leadingEmptyCells = month.atDay(1).dayOfWeek.value - 1
        val cellCount = ((leadingEmptyCells + month.lengthOfMonth() + 6) / 7) * 7

        val dates: List<LocalDate?> = (0 until cellCount).map { index ->
            val dayNumber = index - leadingEmptyCells + 1
            if (dayNumber in 1..month.lengthOfMonth()) {
                month.atDay(dayNumber)
            } else {
                null
            }
        }

        dates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .then(
                                if (date != null) {
                                    Modifier.clickable {
                                        onDateSelected(date)
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                )

                                Spacer(Modifier.size(4.dp))

                                if (date in state.activeDates) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                color = androidx.compose.ui.graphics.Color(
                                                    0xFFFF9800
                                                ),
                                                shape = CircleShape,
                                            )
                                    )
                                } else {
                                    Spacer(Modifier.size(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
