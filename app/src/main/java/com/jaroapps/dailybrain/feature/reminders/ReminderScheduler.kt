package com.jaroapps.dailybrain.feature.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime

/**
 * Schedules non-exact local reminders at 18:00 and 21:00.
 */
class ReminderScheduler(private val context: Context) {

    fun scheduleAll() {
        REMINDER_HOURS.forEach(::scheduleNext)
    }

    fun scheduleNext(hour: Int) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour, 0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            reminderPendingIntent(hour),
        )
    }

    private fun reminderPendingIntent(hour: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_REMINDER_HOUR, hour)
        return PendingIntent.getBroadcast(
            context,
            hour,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val EXTRA_REMINDER_HOUR = "extra_reminder_hour"
        val REMINDER_HOURS = listOf(18, 21)
    }
}
