package com.jaroapps.dailybrain.feature.announcements.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jaroapps.dailybrain.feature.announcements.domain.model.AppAnnouncement
import com.jaroapps.dailybrain.feature.announcements.domain.model.AppAnnouncementType
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

@Composable
fun AnnouncementDialog(
    announcement: AppAnnouncement,
    onDismiss: () -> Unit,
) {
    val payload = remember(announcement.id) {
        runCatching { JSONObject(announcement.payload) }.getOrElse { JSONObject() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 680.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (announcement.type) {
                    AppAnnouncementType.MISSED_STREAK -> MissedStreakContent(payload)
                    AppAnnouncementType.ACHIEVEMENTS -> AchievementContent(payload)
                    AppAnnouncementType.COLLECTIBLE -> CollectibleContent(payload)
                    else -> {
                        Text("🧠", fontSize = 52.sp)
                        Text("Есть новости!", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Продолжить")
                }
            }
        }
    }
}

@Composable
private fun MissedStreakContent(payload: JSONObject) {
    val missedDays = payload.optInt("missedDays").coerceAtLeast(0)
    val usedPatches = payload.optInt("usedPatches").coerceAtLeast(0)
    val remainingPatches = payload.optInt("remainingPatches").coerceAtLeast(0)
    val hadActiveStreak = payload.optBoolean("hadActiveStreak")
    val streakPreserved = payload.optBoolean("streakPreserved")
    val streakActiveAfter = payload.optBoolean("streakActiveAfter")

    Text("🧠", fontSize = 58.sp)
    Text("Не бросай тренировки!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    Text(
        "Ты пропустил " + missedDays + " " + dayWord(missedDays) +
            ". Главное — вернуться и продолжить заботиться о своей памяти и внимании.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (usedPatches > 0 || remainingPatches > 0) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StreakStat("Потрачено", usedPatches.toString(), Modifier.weight(1f))
            StreakStat("Осталось", remainingPatches.toString(), Modifier.weight(1f))
        }
    }
    when {
        hadActiveStreak && streakPreserved -> {
            AnnouncementMessage(
                title = "Серия сохранена!",
                message = "Пластыри защитили пропущенные дни. Продолжай в том же духе.",
                positive = true,
            )
        }
        hadActiveStreak -> {
            AnnouncementMessage(
                title = "Серия прервалась",
                message = when {
                    streakActiveAfter ->
                        "Пластырей не хватило, чтобы сохранить прежнюю серию, но ты уже вернулся к тренировкам. Продолжай новую!"
                    usedPatches > 0 ->
                        "Пластырей не хватило на все пропущенные дни. Не расстраивайся — начни новую серию сегодня."
                    else ->
                        "Пластыри не использовались. Сегодня отличный день, чтобы начать новую серию."
                },
                positive = false,
            )
        }
        streakActiveAfter -> {
            AnnouncementMessage(
                title = "Ты снова тренируешься!",
                message = "Ты уже вернулся к занятиям. Продолжай — новая серия начинается с каждого шага.",
                positive = true,
            )
        }
        else -> {
            AnnouncementMessage(
                title = "Начни новую серию!",
                message = "Каждая тренировка — небольшой шаг вперёд. Возвращайся к занятиям сегодня.",
                positive = true,
            )
        }
    }
}

@Composable
private fun StreakStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AnnouncementMessage(title: String, message: String, positive: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title,
                color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AchievementContent(payload: JSONObject) {
    val items = remember(payload) { payload.optJSONArray("items") ?: JSONArray() }
    Text("🎉", fontSize = 58.sp)
    Text("Поздравляем!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    Text(
        "Отличная тренировка! Вот чего ты сегодня добился:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(items.length()) { index ->
            val item = items.optJSONObject(index) ?: return@repeat
            val (emoji, title, description) = achievementDescription(item)
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(emoji, fontSize = 30.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun achievementDescription(item: JSONObject): Triple<String, String, String> =
    when (item.optString("eventType")) {
        "WEEKLY_XP_MILESTONE" -> {
            val points = item.optInt("valueInt")
            Triple("⭐", "$points очков за неделю", "Ты достиг недельного рубежа!")
        }
        "WEEKLY_STREAK" -> Triple("🏅", "Новая 7-дневная серия", "Получен новый бейджик за непрерывные тренировки.")
        "WEEKLY_CHALLENGE" -> Triple("🎯", "Недельное задание выполнено", "Ты выполнил все условия задания этой недели.")
        "MONTHLY_STREAK" -> Triple("🏆", "Месячная серия завершена", "Ты тренировался каждый день в течение месяца.")
        "PERSONAL_RECORD" -> {
            val duration = item.optLong("valueLong")
            Triple("⏱️", "Личный рекорд!", "Новое лучшее время: " + formatRecordTime(duration) + ".")
        }
        else -> Triple("✨", "Новое достижение", "Продолжай тренироваться и открывать новые награды.")
    }

@Composable
private fun CollectibleContent(payload: JSONObject) {
    val kind = payload.optString("kind")
    val itemName = payload.optString("itemName", "Новый предмет")
    val senderName = payload.optString("senderName", "Пользователь DailyBrain")
    val endorphins = payload.optInt("endorphins")
    val emoji = when (kind) {
        "COSTUME" -> "👕"
        "BACKGROUND" -> "🏞️"
        else -> "🎁"
    }
    Text(emoji, fontSize = 64.sp)
    Text(
        if (kind == "GIFT") "Ты получил подарок!" else "Новый предмет!",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Text(itemName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Text(
        "От пользователя: $senderName",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    if (endorphins > 0) {
        Text(
            "+$endorphins эндорфинов",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun dayWord(value: Int): String = when (value % 100) {
    11, 12, 13, 14 -> "дней"
    else -> when (value % 10) {
        1 -> "день"
        2, 3, 4 -> "дня"
        else -> "дней"
    }
}

private fun formatRecordTime(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
