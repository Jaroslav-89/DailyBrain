package com.jaroapps.dailybrain.feature.brain

import android.content.Context

/**
 * Dynamic launcher icon switching is temporarily disabled while debugging app startup.
 * The icon assets and manifest aliases are retained for re-enabling this feature later.
 */
object AppIconController {
    @Suppress("UNUSED_PARAMETER")
    fun apply(context: Context, mood: BrainMood) {
        // Intentionally do nothing: MainActivity uses the standard mipmap launcher icon.
    }
}
