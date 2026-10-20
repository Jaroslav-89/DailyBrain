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
) {
    val effectiveDisplayName: String
        get() = displayName?.trim()?.takeIf(String::isNotEmpty) ?: generatedAlias
}
