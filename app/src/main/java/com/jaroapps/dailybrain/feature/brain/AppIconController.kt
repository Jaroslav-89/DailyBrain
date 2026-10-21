package com.jaroapps.dailybrain.feature.brain

import android.content.Context

/**
 * Launcher icon switching is temporarily disabled while stabilizing app startup.
 * The manifest aliases and icon assets are retained for a later isolated test.
 */
object AppIconController {
    @Suppress("UNUSED_PARAMETER")
    fun apply(context: Context, mood: BrainMood) {
        // Intentionally do nothing: changing the enabled launcher alias while
        // MainActivity is resumed can cause some launchers/devices to close the task.
    }
}
