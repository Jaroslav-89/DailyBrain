package com.jaroapps.dailybrain.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jaroapps.dailybrain.core.database.dao.TrainingSessionDao
import com.jaroapps.dailybrain.core.database.entity.TrainingSessionEntity

@Database(
    entities = [TrainingSessionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trainingSessionDao(): TrainingSessionDao
}