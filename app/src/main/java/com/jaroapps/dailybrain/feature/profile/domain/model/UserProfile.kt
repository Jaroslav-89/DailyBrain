package com.jaroapps.dailybrain.feature.profile.domain.model

import java.time.LocalDate

enum class UserGender {
    FEMALE,
    MALE,
}

data class UserProfile(
    val localAccountId: String,
    val firebaseUid: String? = null,
    val displayName: String? = null,
    val generatedAlias: String,
    val gender: UserGender? = null,
    val birthDate: LocalDate? = null,
    val country: String? = null,
    val city: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val googleSubject: String? = null,
    val googleEmail: String? = null,
    val googleDisplayName: String? = null,
    val googlePhotoUrl: String? = null,
    val username: String? = null,
    val inviteCode: String? = null,
    val xpPoints: Int = 0,
    val endorphinBalance: Int = 0,
    val patchCount: Int = 0,
    val profileInfoVisible: Boolean = true,
    val feedUpdatesEnabled: Boolean = true,
) {
    val effectiveDisplayName: String
        get() = displayName?.trim()?.takeIf(String::isNotEmpty) ?: generatedAlias
}
