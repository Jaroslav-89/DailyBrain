package com.jaroapps.dailybrain.common.domain.service

fun interface TrainingStateChangedNotifier {
    suspend fun onTrainingStateChanged()
}
