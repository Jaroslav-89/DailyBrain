package com.jaroapps.dailybrain.core.presentation

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.jaroapps.dailybrain.R
import java.util.concurrent.ConcurrentHashMap

/** Centralized, preference-aware sound and haptic feedback for exercise and announcement events. */
class FeedbackController(context: Context) {
    private val appContext = context.applicationContext
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    private val loadedSounds = ConcurrentHashMap.newKeySet<Int>()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val exerciseSoundId: Int
    private val announcementSoundId: Int

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedSounds.add(sampleId)
        }
        exerciseSoundId = soundPool.load(appContext, R.raw.exercise_complete_fanfare, 1)
        announcementSoundId = soundPool.load(appContext, R.raw.announcement_fanfare, 1)
    }

    fun onCountdownDigit() {
        vibrate(shortPulse())
    }

    fun onExerciseCompleted() {
        vibrate(completionPattern())
        play(exerciseSoundId)
    }

    fun onAnnouncementShown(playFanfare: Boolean) {
        vibrate(completionPattern())
        if (playFanfare) play(announcementSoundId)
    }

    private fun play(soundId: Int) {
        if (!FeedbackPreferences.soundEnabled || !loadedSounds.contains(soundId) || soundId == 0) return
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    private fun vibrate(effect: HapticPattern) {
        if (!FeedbackPreferences.vibrationEnabled || vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(effect.timings, effect.amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect.timings, -1)
        }
    }

    private data class HapticPattern(val timings: LongArray, val amplitudes: IntArray)

    private fun shortPulse() = HapticPattern(
        timings = longArrayOf(0L, 38L),
        amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE),
    )

    private fun completionPattern() = HapticPattern(
        timings = longArrayOf(0L, 48L, 85L, 175L),
        amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
    )
}
