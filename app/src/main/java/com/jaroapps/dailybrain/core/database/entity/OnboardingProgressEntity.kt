package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "onboarding_progress")
data class OnboardingProgressEntity(
    @PrimaryKey val localAccountId: String,
    val hasSeenAppIntro: Boolean = false,
    val hasSeenArithmeticIntro: Boolean = false,
    val hasSeenMemoryIntro: Boolean = false,
    val hasSeenCountingIntro: Boolean = false,
    val hasSeenStroopIntro: Boolean = false,
)
