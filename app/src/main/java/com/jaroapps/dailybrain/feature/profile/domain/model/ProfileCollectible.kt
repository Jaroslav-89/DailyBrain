package com.jaroapps.dailybrain.feature.profile.domain.model

enum class ProfileCollectibleKind {
    GIFT,
    BADGE,
}

/** Public-safe, read-only projection of a received gift or an earned badge. */
data class ProfileCollectible(
    val id: String,
    val kind: ProfileCollectibleKind,
    val itemType: String,
    val acquiredAtEpochMs: Long,
    val senderFirebaseUid: String? = null,
    val senderName: String? = null,
    val periodKey: String? = null,
    val triggerWorkoutId: Long? = null,
    val amount: Int? = null,
)
