package com.jaroapps.dailybrain.feature.brain

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Switches between the base and sour launcher aliases. */
object AppIconController {

    private const val LAUNCHER_BASE = "com.jaroapps.dailybrain.LauncherBase"
    private const val LAUNCHER_SOUR = "com.jaroapps.dailybrain.LauncherSour"

    fun apply(context: Context, mood: BrainMood) {
        val packageManager = context.packageManager
        val activeAlias = if (mood == BrainMood.SOUR) LAUNCHER_SOUR else LAUNCHER_BASE
        val inactiveAlias = if (activeAlias == LAUNCHER_BASE) LAUNCHER_SOUR else LAUNCHER_BASE

        // Enable the destination first so an alias switch never temporarily leaves both disabled.
        setAliasState(packageManager, context, activeAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        setAliasState(packageManager, context, inactiveAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
    }

    private fun setAliasState(
        packageManager: PackageManager,
        context: Context,
        alias: String,
        desiredState: Int,
    ) {
        val component = ComponentName(context, alias)
        if (packageManager.getComponentEnabledSetting(component) != desiredState) {
            packageManager.setComponentEnabledSetting(
                component,
                desiredState,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
