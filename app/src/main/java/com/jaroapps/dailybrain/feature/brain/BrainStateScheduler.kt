package com.jaroapps.dailybrain.feature.brain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId

class BrainStateScheduler(private val context: Context) {
    fun scheduleNext() {
        val now = LocalDateTime.now()
        val todayAtEight = now.toLocalDate().atTime(20, 0)
        val next = if (now.isBefore(todayAtEight)) {
            todayAtEight
        } else {
            now.toLocalDate().plusDays(1).atStartOfDay()
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, BrainStateReceiver::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    companion object {
        const val ACTION_REFRESH = "com.jaroapps.dailybrain.action.REFRESH_BRAIN_STATE"
        private const val REQUEST_CODE = 2020
    }
}
