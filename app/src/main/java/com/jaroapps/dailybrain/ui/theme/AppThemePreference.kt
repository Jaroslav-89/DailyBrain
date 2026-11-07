package com.jaroapps.dailybrain.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Stores the dark-theme choice per local profile.
 */
object AppThemePreference {
    private const val PREFERENCES_NAME = "app_appearance"
    private const val KEY_DARK_THEME_ENABLED = "dark_theme_enabled"
    private const val KEY_LEGACY_MIGRATED = "profile_scoped_migrated"
    private const val PROFILE_KEY_PREFIX = "dark_theme_enabled_"

    var isDarkTheme by mutableStateOf(false)
        private set

    private var activeProfileId: String? = null

    fun ensureLoaded(context: Context, localAccountId: String?) {
        val profileId = localAccountId?.takeIf(String::isNotBlank)
        if (activeProfileId == profileId) return
        activeProfileId = profileId

        val preferences = context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val key = profileKey(profileId)
        if (!preferences.contains(key)) {
            val canMigrateLegacy = profileId != null &&
                !preferences.getBoolean(KEY_LEGACY_MIGRATED, false)
            val initialValue = if (canMigrateLegacy && preferences.contains(KEY_DARK_THEME_ENABLED)) {
                preferences.getBoolean(KEY_DARK_THEME_ENABLED, false)
            } else {
                false
            }
            preferences.edit().putBoolean(key, initialValue).apply()
            if (canMigrateLegacy) preferences.edit().putBoolean(KEY_LEGACY_MIGRATED, true).apply()
        }
        isDarkTheme = preferences.getBoolean(key, false)
    }

    fun setDarkTheme(context: Context, enabled: Boolean) {
        val preferences = context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.edit().putBoolean(profileKey(activeProfileId), enabled).apply()
        isDarkTheme = enabled
    }

    private fun profileKey(localAccountId: String?): String =
        PROFILE_KEY_PREFIX + (localAccountId ?: "no_active_profile")
}
