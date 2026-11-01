package com.jaroapps.dailybrain.feature.rewards.presentation

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.rewards.domain.model.RewardBalanceType
import com.jaroapps.dailybrain.feature.rewards.domain.model.RewardHistoryEntry
import com.jaroapps.dailybrain.feature.rewards.domain.model.RewardHistoryWeekSection
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val RewardHistoryDateFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm", Locale.getDefault())
private val RewardHistoryWeekFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardHistoryScreen(
    onBack: () -> Unit,
    viewModel: RewardHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val titleRes = when (state.balanceType) {
        RewardBalanceType.XP -> R.string.reward_history_title_xp
        RewardBalanceType.ENDORPHINS -> R.string.reward_history_title_endorphins
    }
    val unitRes = when (state.balanceType) {
        RewardBalanceType.XP -> R.string.reward_history_unit_xp
        RewardBalanceType.ENDORPHINS -> R.string.reward_history_unit_endorphins
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.reward_history_current_balance),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.balance.toString(),
                            fontSize = 48.sp,
                            lineHeight = 54.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Icon(
                            imageVector = when (state.balanceType) {
                                RewardBalanceType.XP -> Icons.Filled.Star
                                RewardBalanceType.ENDORPHINS -> Icons.Filled.Favorite
                            },
                            contentDescription = null,
                            tint = when (state.balanceType) {
                                RewardBalanceType.XP -> Color(0xFF43A047)
                                RewardBalanceType.ENDORPHINS -> Color(0xFF8E24AA)
                            },
                            modifier = Modifier.padding(start = 10.dp).size(34.dp),
                        )
                    }
                    Text(
                        stringResource(
                            if (state.balanceType == RewardBalanceType.XP) {
                                R.string.reward_history_unit_xp
                            } else {
                                R.string.reward_history_unit_endorphins
                            },
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.weekSections.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.reward_history_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.weekSections.forEach { section ->
                        item(key = "week-${section.weekStart}") {
                            RewardHistoryWeekHeader(
                                section = section,
                                unitRes = unitRes,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        items(section.entries, key = { it.id }) { entry ->
                            RewardHistoryEntryCard(
                                entry = entry,
                                unitRes = unitRes,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RewardHistoryWeekHeader(
    section: RewardHistoryWeekSection,
    unitRes: Int,
    modifier: Modifier = Modifier,
) {
    val weekRange = "${section.weekStart.format(RewardHistoryWeekFormatter)} — ${section.weekEnd.format(RewardHistoryWeekFormatter)}"
    val totalText = when {
        section.totalAmount > 0L -> "+${section.totalAmount}"
        section.totalAmount < 0L -> "−${kotlin.math.abs(section.totalAmount)}"
        else -> "0"
    }
    val totalColor = when {
        section.totalAmount > 0L -> Color(0xFF2E7D32)
        section.totalAmount < 0L -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            stringResource(R.string.reward_history_week_title, weekRange),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(R.string.reward_history_week_change, totalText, stringResource(unitRes)),
            style = MaterialTheme.typography.labelMedium,
            color = totalColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RewardHistoryEntryCard(
    entry: RewardHistoryEntry,
    unitRes: Int,
    modifier: Modifier = Modifier,
) {
    val amountText = when {
        entry.amount > 0 -> "+${entry.amount}"
        entry.amount < 0 -> "−${abs(entry.amount.toLong())}"
        else -> "0"
    }
    val reasonRes = when (entry.reason) {
        "FIRST_ARITHMETIC_OF_DAY" -> R.string.reward_reason_first_arithmetic
        "REPEAT_ARITHMETIC_OF_DAY" -> R.string.reward_reason_repeat_arithmetic
        "FIRST_EXERCISE_TYPE_OF_WEEK" -> R.string.reward_reason_first_exercise_week
        "WEEKLY_STREAK" -> R.string.reward_reason_weekly_streak
        "WEEKLY_CHALLENGE" -> R.string.reward_reason_weekly_challenge
        "MONTHLY_STREAK" -> R.string.reward_reason_monthly_streak
        "ARITHMETIC_PERSONAL_RECORD" -> R.string.reward_reason_personal_record
        "PATCH_PURCHASE" -> R.string.reward_reason_patch_purchase
        "GIFT_PURCHASE" -> R.string.reward_reason_gift_purchase
        "COSTUME_PURCHASE" -> R.string.reward_reason_costume_purchase
        "BACKGROUND_PURCHASE" -> R.string.reward_reason_background_purchase
        "GIFT_RECEIVED_CANDY" -> R.string.reward_reason_gift_candy
        "GIFT_RECEIVED_COFFEE" -> R.string.reward_reason_gift_coffee
        "GIFT_RECEIVED_CROISSANT" -> R.string.reward_reason_gift_croissant
        "GIFT_RECEIVED_HAMBURGER" -> R.string.reward_reason_gift_hamburger
        "GIFT_RECEIVED_CAKE" -> R.string.reward_reason_gift_cake
        else -> R.string.reward_history_reason_other
    }
    val exerciseRes = when (entry.exerciseType) {
        "ARITHMETIC" -> R.string.exercise_arithmetic
        "MEMORY_WORDS" -> R.string.exercise_memory
        "STROOP_TEST" -> R.string.exercise_stroop
        "COUNTING_TRAINING" -> R.string.exercise_counting
        else -> null
    }
    val recordDurationText = if (
        entry.reason == "ARITHMETIC_PERSONAL_RECORD" && entry.resultDurationMs != null
    ) {
        rewardRecordDurationText(entry.resultDurationMs)
    } else {
        null
    }
    val reasonText = if (recordDurationText != null) {
        stringResource(R.string.reward_reason_personal_record_with_time, recordDurationText)
    } else {
        stringResource(reasonRes)
    }
    val dateText = runCatching {
        Instant.ofEpochMilli(entry.occurredAtEpochMs)
            .atZone(ZoneId.systemDefault())
            .format(RewardHistoryDateFormatter)
    }.getOrDefault("")

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(reasonText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (exerciseRes != null) {
                    Text(
                        stringResource(R.string.reward_history_exercise, stringResource(exerciseRes)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(dateText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "$amountText ${stringResource(unitRes)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    entry.amount > 0 -> Color(0xFF2E7D32)
                    entry.amount < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}


@Composable
private fun rewardRecordDurationText(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L

    return when {
        hours > 0L -> stringResource(
            R.string.reward_history_duration_hours,
            hours.toInt(),
            minutes.toInt(),
            seconds.toInt(),
        )
        totalSeconds >= 60L -> stringResource(
            R.string.reward_history_duration_minutes,
            minutes.toInt(),
            seconds.toInt(),
        )
        else -> stringResource(R.string.reward_history_duration_seconds, seconds.toInt())
    }
}
