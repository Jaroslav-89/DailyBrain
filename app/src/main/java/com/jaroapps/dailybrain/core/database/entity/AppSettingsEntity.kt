package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val activeLocalAccountId: String,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}