package com.jaroapps.dailybrain.feature.brain

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Switches the app's launcher icon to match the current brain mood.
 * Only one launcher alias is enabled at a time.
 */
object AppIconController {

    private const val LAUNCHER_BASE = "com.jaroapps.dailybrain.LauncherBase"
    private const val LAUNCHER_SOUR = "com.jaroapps.dailybrain.LauncherSour"
    private const val LAUNCHER_INSPIRED = "com.jaroapps.dailybrain.LauncherInspired"

    fun apply(context: Context, mood: BrainMood) {
        val packageManager = context.packageManager
        val activeAlias = when (mood) {
            BrainMood.BASE -> LAUNCHER_BASE
            BrainMood.SOUR -> LAUNCHER_SOUR
            BrainMood.INSPIRED -> LAUNCHER_INSPIRED
        }

        listOf(LAUNCHER_BASE, LAUNCHER_SOUR, LAUNCHER_INSPIRED).forEach { alias ->
            packageManager.setComponentEnabledSetting(
                ComponentName(context, alias),
                if (alias == activeAlias) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
