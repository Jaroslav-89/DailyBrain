package com.jaroapps.dailybrain.feature.home.presentation

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Stores the home screen background preference per local profile.
 */
object HomeAppearancePreference {
    private const val PREFERENCES_NAME = "home_appearance"
    private const val KEY_DYNAMIC_BACKGROUND = "dynamic_background_enabled"
    private const val KEY_LEGACY_MIGRATED = "profile_scoped_migrated"
    private const val PROFILE_KEY_PREFIX = "dynamic_background_enabled_"

    var dynamicBackgroundEnabled by mutableStateOf(true)
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
            val initialValue = if (canMigrateLegacy && preferences.contains(KEY_DYNAMIC_BACKGROUND)) {
                preferences.getBoolean(KEY_DYNAMIC_BACKGROUND, true)
            } else {
                true
            }
            preferences.edit().putBoolean(key, initialValue).apply()
            if (canMigrateLegacy) preferences.edit().putBoolean(KEY_LEGACY_MIGRATED, true).apply()
        }
        dynamicBackgroundEnabled = preferences.getBoolean(key, true)
    }

    fun setDynamicBackgroundEnabled(context: Context, enabled: Boolean) {
        val preferences = context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.edit().putBoolean(profileKey(activeProfileId), enabled).apply()
        dynamicBackgroundEnabled = enabled
    }

    private fun profileKey(localAccountId: String?): String =
        PROFILE_KEY_PREFIX + (localAccountId ?: "no_active_profile")
}
