package com.jaroapps.dailybrain.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val activeLocalAccountId: String,
    @ColumnInfo(defaultValue = "0") val profileSelectionRequired: Boolean = false,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}