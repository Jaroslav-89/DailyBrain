package com.jaroapps.dailybrain.feature.profile.domain.model

/** Public-safe projection returned by an exact username lookup. */
data class PublicUserProfile(
    val firebaseUid: String,
    val username: String,
    val displayName: String?,
    val avatarUrl: String?,
)
