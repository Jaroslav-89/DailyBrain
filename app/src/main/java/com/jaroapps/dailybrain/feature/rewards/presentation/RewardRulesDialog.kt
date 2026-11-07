package com.jaroapps.dailybrain.feature.rewards.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.feature.rewards.domain.model.MonthlyChallengeRules

enum class RewardRulesTopic {
    STREAKS,
    EXPERIENCE,
    ENDORPHINS,
}

private data class RewardRuleSection(
    val title: String,
    val description: String,
)

@Composable
fun RewardRulesDialog(
    topic: RewardRulesTopic,
    onDismiss: () -> Unit,
) {
    val title = when (topic) {
        RewardRulesTopic.STREAKS -> "Правила серий и заданий"
        RewardRulesTopic.EXPERIENCE -> "Как получать очки опыта"
        RewardRulesTopic.ENDORPHINS -> "Как получать эндорфины"
    }
    val sections = when (topic) {
        RewardRulesTopic.STREAKS -> listOf(
            RewardRuleSection(
                "Непрерывная серия",
                "Выполняйте хотя бы одно упражнение каждый день. Эндорфиновый пластырь тоже сохраняет день серии. За каждые 7 последовательных дней начисляется 100 очков опыта, 10 эндорфинов и выдаётся бейджик. Бейджики за следующие семидневные отрезки также суммируются.",
            ),
            RewardRuleSection(
                "Месячная серия",
                "Чтобы получить месячный бейджик серии, каждый календарный день месяца должен быть закрыт тренировкой или пластырем. Награда: 500 очков опыта и 50 эндорфинов.",
            ),
            RewardRuleSection(
                "Задание на неделю",
                "В течение недели с понедельника по воскресенье завершите 7 арифметических тренировок, 1 тренировку памяти, 1 тест Струпа и 1 тренировку устного счёта. Пластырь не заменяет упражнение. Награда: 100 очков опыта, 10 эндорфинов и бейджик.",
            ),
            RewardRuleSection(
                "Задание на месяц",
                monthlyChallengeDescription(),
            ),
        )
        RewardRulesTopic.EXPERIENCE -> listOf(
            RewardRuleSection("Арифметика", "Первая завершённая арифметика за календарный день приносит 50 очков. Каждая следующая арифметика в этот день — ещё 10 очков."),
            RewardRuleSection("Другие упражнения", "Первое завершение каждого дополнительного вида упражнений за календарную неделю приносит 50 очков. Повторные прохождения того же вида в эту неделю — 0 очков."),
            RewardRuleSection("Задание на неделю", "100 очков опыта и бейджик за выполнение недельного задания."),
            RewardRuleSection("Непрерывная серия", "100 очков опыта и бейджик за каждые 7 последовательных дней с упражнением или пластырём."),
            RewardRuleSection("Месячная серия", "500 очков опыта и уникальный бейджик за каждый календарный месяц без пропусков."),
            RewardRuleSection("Задание на месяц", monthlyChallengeDescription()),
            RewardRuleSection("Рекорд в арифметике", "100 очков за новый личный рекорд. Первые 7 арифметических тренировок не учитываются; точность должна быть не ниже 95%, а первая недельная серия уже должна быть завершена."),
        )
        RewardRulesTopic.ENDORPHINS -> listOf(
            RewardRuleSection("Недельная серия", "+10 эндорфинов за каждые 7 последовательных дней с упражнением или пластырём."),
            RewardRuleSection("Задание на неделю", "+10 эндорфинов за выполнение недельного задания."),
            RewardRuleSection("Рекорд в арифметике", "+10 эндорфинов за новый личный рекорд."),
            RewardRuleSection("Месячная серия", "+50 эндорфинов за каждый календарный месяц без пропусков."),
            RewardRuleSection("Подарки", "При получении подарка от другого пользователя эндорфины начисляются автоматически. Количество зависит от подарка."),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                sections.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(section.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(section.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Понятно") }
        },
    )
}

private fun monthlyChallengeDescription(): String {
    val requirements = MonthlyChallengeRules.DEFAULT_MONTHLY_CHALLENGE_REQUIREMENTS
    val targets = requirements.joinToString(separator = ", ") { requirement ->
        val label = when (ExerciseType.fromStorage(requirement.exerciseType)) {
            ExerciseType.ARITHMETIC -> "арифметических тренировок"
            ExerciseType.MEMORY_WORDS -> "тренировки памяти"
            ExerciseType.STROOP_TEST -> "теста Струпа"
            ExerciseType.COUNTING_TRAINING -> "тренировки устного счёта"
            null -> "упражнений «${requirement.exerciseType}»"
        }
        "${requirement.requiredCount} $label"
    }
    return "За календарный месяц наберите общий объём упражнений за четыре недели: $targets. " +
        "Не нужно выполнять недельные задания по отдельности: учитывается общая сумма за месяц. " +
        "Пластырь не считается упражнением. Награда: ${MonthlyChallengeRules.XP_REWARD} очков опыта и уникальный месячный бейджик."
}
