package com.jaroapps.dailybrain.feature.brain

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.jaroapps.dailybrain.MainActivity
import com.jaroapps.dailybrain.R

object BrainWidgetUpdater {
    fun update(context: Context, mood: BrainMood, streak: Int, hasTrainingToday: Boolean) {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, BrainWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(provider)
        if (ids.isEmpty()) return

        val image = when (mood) {
            BrainMood.BASE -> R.drawable.brain_base
            BrainMood.SOUR -> R.drawable.brain_sour
        }
        val title = if (hasTrainingToday) "Тренировка" else "Пройти"
        val subtitle = if (hasTrainingToday) "пройдена" else "тренировку"
        val streakColor = if (hasTrainingToday) 0xFFFF9800.toInt() else 0xFF9E9E9E.toInt()
        val streakBackground = if (hasTrainingToday) {
            R.drawable.widget_streak_circle_orange
        } else {
            R.drawable.widget_streak_circle_gray
        }

        val openApp = PendingIntent.getActivity(
            context,
            7301,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val views = RemoteViews(context.packageName, R.layout.widget_brain).apply {
            setImageViewResource(R.id.brainImage, image)
            setTextViewText(R.id.widgetTitle, title)
            setTextViewText(R.id.widgetSubtitle, subtitle)
            setTextViewText(R.id.widgetStreak, streak.toString())
            setTextColor(R.id.widgetStreak, streakColor)
            setInt(R.id.widgetStreak, "setBackgroundResource", streakBackground)
            setOnClickPendingIntent(R.id.widgetRoot, openApp)
        }
        manager.updateAppWidget(ids, views)
    }
}
