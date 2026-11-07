package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak_patch_state")
data class StreakPatchStateEntity(
    @PrimaryKey val userId: String,
    /** Last local calendar date evaluated by the automatic patch processor (yyyyMMdd). */
    val lastProcessedDate: Int,
    /** True when the last processed day belongs to an unbroken streak. */
    val isStreakActive: Boolean,
)
