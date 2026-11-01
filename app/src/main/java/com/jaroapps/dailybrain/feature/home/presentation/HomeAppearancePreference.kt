package com.jaroapps.dailybrain.feature.home.presentation

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Stores the home screen background preference on this device and exposes it as Compose state,
 * so the home screen reacts immediately when the setting is changed.
 */
object HomeAppearancePreference {
    private const val PREFERENCES_NAME = "home_appearance"
    private const val KEY_DYNAMIC_BACKGROUND = "dynamic_background_enabled"

    var dynamicBackgroundEnabled by mutableStateOf(true)
        private set

    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        dynamicBackgroundEnabled = context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DYNAMIC_BACKGROUND, true)
        loaded = true
    }

    fun setDynamicBackgroundEnabled(context: Context, enabled: Boolean) {
        ensureLoaded(context)
        context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DYNAMIC_BACKGROUND, enabled)
            .apply()
        dynamicBackgroundEnabled = enabled
    }
}
