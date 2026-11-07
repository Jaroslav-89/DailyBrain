package com.jaroapps.dailybrain.feature.profile.domain.model

/** Account-level privacy controls synchronized through the authenticated cloud profile. */
data class UserPrivacySettings(
    val profileInfoVisible: Boolean = true,
    val feedUpdatesEnabled: Boolean = true,
)
