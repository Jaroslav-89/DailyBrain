package com.jaroapps.dailybrain.feature.history.domain.repository

import kotlinx.coroutines.flow.Flow

interface TrainingCalendarRepository {
    fun observeActiveDates(): Flow<Set<Int>>
}
