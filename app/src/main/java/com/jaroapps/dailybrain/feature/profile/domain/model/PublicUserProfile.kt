package com.jaroapps.dailybrain.feature.profile.domain.model

/** Public-safe profile data. It intentionally contains no Google email, legal name, or birth date. */
data class PublicUserProfile(
    val firebaseUid: String,
    val username: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val avatarConfigurationJson: String? = null,
    val country: String? = null,
    val city: String? = null,
    val gender: String? = null,
    val memberSinceEpochMs: Long? = null,
    val statistics: PublicUserStatistics = PublicUserStatistics(),
    val isRestricted: Boolean = false,
    val profileInfoVisible: Boolean = true,
)

data class PublicUserStatistics(
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val totalExerciseCount: Int = 0,
    val lastSevenDaysExerciseCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val firstTrainingDate: String? = null,
    val bestExerciseStats: List<PublicBestExerciseResult> = emptyList(),
)

data class PublicBestExerciseResult(
    val exerciseType: String,
    val bestDurationMs: Long? = null,
    val bestCorrectCount: Int? = null,
)
