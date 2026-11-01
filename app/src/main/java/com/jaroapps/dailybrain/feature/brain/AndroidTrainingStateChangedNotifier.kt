package com.jaroapps.dailybrain.feature.brain

import android.util.Log
import com.jaroapps.dailybrain.common.domain.service.TrainingStateChangedNotifier

class AndroidTrainingStateChangedNotifier(
    private val brainStateManager: BrainStateManager,
) : TrainingStateChangedNotifier {
    override suspend fun onTrainingStateChanged() {
        try {
            brainStateManager.refresh()
        } catch (error: Exception) {
            Log.w(TAG, "Could not refresh brain mood after training save", error)
        }
    }

    private companion object {
        const val TAG = "BrainMood"
    }
}
