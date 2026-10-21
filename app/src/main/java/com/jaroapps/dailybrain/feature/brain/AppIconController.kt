package com.jaroapps.dailybrain.feature.brain

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Isolated launcher-alias test: toggle only the sour shortcut.
 * MainActivity remains the primary launcher entry during this test.
 */
object AppIconController {

    private const val LAUNCHER_SOUR = "com.jaroapps.dailybrain.LauncherSour"

    fun apply(context: Context, mood: BrainMood) {
        val packageManager = context.packageManager
        val sourShouldBeEnabled = mood == BrainMood.SOUR
        val sourComponent = ComponentName(context, LAUNCHER_SOUR)
        val currentState = packageManager.getComponentEnabledSetting(sourComponent)
        val desiredState = if (sourShouldBeEnabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        if (currentState != desiredState) {
            packageManager.setComponentEnabledSetting(
                sourComponent,
                desiredState,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
