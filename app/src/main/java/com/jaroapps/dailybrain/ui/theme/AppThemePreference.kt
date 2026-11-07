package com.jaroapps.dailybrain.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App-wide theme choice. It is device-scoped rather than tied to a local profile,
 * so the selected appearance is restored before the first Compose frame on startup.
 */
object AppThemePreference {
    private const val PREFERENCES_NAME = "app_appearance"
    private const val KEY_DARK_THEME_ENABLED = "dark_theme_enabled"

    var isDarkTheme by mutableStateOf(false)
        private set

    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        val preferences = context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        isDarkTheme = preferences.getBoolean(KEY_DARK_THEME_ENABLED, false)
        loaded = true
    }

    fun setDarkTheme(context: Context, enabled: Boolean) {
        ensureLoaded(context)
        context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_THEME_ENABLED, enabled)
            .apply()
        isDarkTheme = enabled
    }
}
