package com.jaroapps.dailybrain.feature.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jaroapps.dailybrain.MainActivity
import com.jaroapps.dailybrain.feature.brain.BrainStateScheduler
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = ReminderScheduler(context.applicationContext)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                scheduler.scheduleAll()
                BrainStateScheduler(context.applicationContext).scheduleNext()
                return
            }
        }

        val hour = intent.getIntExtra(ReminderScheduler.EXTRA_REMINDER_HOUR, -1)
        if (hour !in ReminderScheduler.REMINDER_HOURS) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val koin = GlobalContext.get()
                val database = koin.get<AppDatabase>()
                val activeUserProvider = koin.get<ActiveUserProvider>()
                val userId = activeUserProvider.currentUserId()
                val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE).toInt()
                val hasTrainingToday = database.trainingDayDao().hasTrainingOnDate(userId, today)

                if (!hasTrainingToday) {
                    showReminder(context.applicationContext, hour)
                }
            } catch (_: Exception) {
                // Keep the receiver resilient if the database or dependency graph is temporarily unavailable.
            } finally {
                scheduler.scheduleNext(hour)
                pendingResult.finish()
                scope.cancel()
            }
        }
    }

    private fun showReminder(context: Context, hour: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }

        val openAppIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val message = if (hour == 18) {
            R.string.reminder_six_hours_message
        } else {
            R.string.reminder_three_hours_message
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(message))
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + hour, notification)
    }

    companion object {
        private const val CHANNEL_ID = "daily_training_reminders"
        private const val NOTIFICATION_ID = 1800
    }
}
