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
        val status = if (hasTrainingToday) {
            R.string.home_training_done_today
        } else {
            R.string.home_not_trained_today
        }

        val openApp = PendingIntent.getActivity(
            context,
            7301,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val views = RemoteViews(context.packageName, R.layout.widget_brain).apply {
            setImageViewResource(R.id.brainImage, image)
            setTextViewText(R.id.widgetTitle, context.getString(R.string.app_name))
            setTextViewText(R.id.widgetSubtitle, context.getString(status))
            setTextViewText(R.id.widgetStreak, streak.toString())
            setOnClickPendingIntent(R.id.widgetRoot, openApp)
        }
        manager.updateAppWidget(ids, views)
    }
}
