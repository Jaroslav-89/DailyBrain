package com.jaroapps.dailybrain.feature.brain

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Two-state launcher alias test.
 * MainActivity remains a fallback launcher entry while BASE and SOUR are tested.
 */
object AppIconController {

    private const val LAUNCHER_BASE = "com.jaroapps.dailybrain.LauncherBase"
    private const val LAUNCHER_SOUR = "com.jaroapps.dailybrain.LauncherSour"

    fun apply(context: Context, mood: BrainMood) {
        val packageManager = context.packageManager
        val activeAlias = if (mood == BrainMood.SOUR) LAUNCHER_SOUR else LAUNCHER_BASE

        listOf(LAUNCHER_BASE, LAUNCHER_SOUR).forEach { alias ->
            val component = ComponentName(context, alias)
            val desiredState = if (alias == activeAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            if (packageManager.getComponentEnabledSetting(component) != desiredState) {
                packageManager.setComponentEnabledSetting(
                    component,
                    desiredState,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
