package com.jaroapps.dailybrain.feature.history.data

import com.jaroapps.dailybrain.core.database.dao.TrainingDayDao
import com.jaroapps.dailybrain.feature.history.domain.repository.TrainingCalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrainingCalendarRepositoryImpl(
    private val dao: TrainingDayDao,
) : TrainingCalendarRepository {

    override fun observeActiveDates(): Flow<Set<Int>> {
        return dao.observeAll().map { days ->
            days.map { it.date }.toSet()
        }
    }
}
