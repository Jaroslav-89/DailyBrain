package com.jaroapps.dailybrain.core.presentation

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Device interaction preferences are stored independently for every local profile. */
object FeedbackPreferences {
    private const val PREFERENCES_NAME = "interaction_preferences"
    private const val SOUND_KEY_PREFIX = "sound_enabled_"
    private const val VIBRATION_KEY_PREFIX = "vibration_enabled_"
    private const val LEFT_HANDED_KEY_PREFIX = "left_handed_"

    var soundEnabled by mutableStateOf(true)
        private set
    var vibrationEnabled by mutableStateOf(true)
        private set
    var isLeftHanded by mutableStateOf(false)
        private set

    private var loadedProfileId: String? = null
    private var hasLoaded = false

    fun ensureLoaded(context: Context, localAccountId: String?) {
        val profileId = localAccountId?.takeIf(String::isNotBlank)
        if (hasLoaded && loadedProfileId == profileId) return

        loadedProfileId = profileId
        hasLoaded = true
        val preferences = context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val suffix = profileId ?: "no_active_profile"
        soundEnabled = preferences.getBoolean(SOUND_KEY_PREFIX + suffix, true)
        vibrationEnabled = preferences.getBoolean(VIBRATION_KEY_PREFIX + suffix, true)
        isLeftHanded = preferences.getBoolean(LEFT_HANDED_KEY_PREFIX + suffix, false)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        saveBoolean(context, SOUND_KEY_PREFIX, enabled)
        soundEnabled = enabled
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        saveBoolean(context, VIBRATION_KEY_PREFIX, enabled)
        vibrationEnabled = enabled
    }

    fun setLeftHanded(context: Context, enabled: Boolean) {
        saveBoolean(context, LEFT_HANDED_KEY_PREFIX, enabled)
        isLeftHanded = enabled
    }

    private fun saveBoolean(context: Context, keyPrefix: String, value: Boolean) {
        val suffix = loadedProfileId ?: "no_active_profile"
        context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(keyPrefix + suffix, value)
            .apply()
    }
}
