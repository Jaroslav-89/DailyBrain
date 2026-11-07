package com.jaroapps.dailybrain.feature.profile.domain.model

enum class PendingProfileDeletionStatus {
    CLOUD_CLEANUP_PENDING,
    CLOUD_DATA_DELETED,
}

data class PendingProfileDeletion(
    val firebaseUid: String,
    val googleSubject: String?,
    val status: PendingProfileDeletionStatus,
)
