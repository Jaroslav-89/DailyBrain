package com.jaroapps.dailybrain.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jaroapps.dailybrain.core.database.dao.TrainingSessionDao
import com.jaroapps.dailybrain.core.database.entity.TrainingSessionEntity
import com.jaroapps.dailybrain.core.database.dao.TrainingDayDao
import com.jaroapps.dailybrain.core.database.entity.TrainingDayEntity

@Database(
    entities = [
        TrainingSessionEntity::class,
        TrainingDayEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun trainingDayDao(): TrainingDayDao
}