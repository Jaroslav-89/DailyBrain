package com.jaroapps.dailybrain.feature.brain

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIconController {
    private val aliases = mapOf(
        BrainMood.BASE to ".LauncherBase",
        BrainMood.SOUR to ".LauncherSour",
        BrainMood.INSPIRED to ".LauncherInspired",
    )

    fun apply(context: Context, mood: BrainMood) {
        val packageManager = context.packageManager
        aliases.forEach { (aliasMood, className) ->
            val state = if (aliasMood == mood) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(
                ComponentName(context.packageName, context.packageName + className),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
