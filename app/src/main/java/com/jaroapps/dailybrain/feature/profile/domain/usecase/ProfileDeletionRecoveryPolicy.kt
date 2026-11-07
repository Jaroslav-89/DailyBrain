package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.model.PendingProfileDeletion
import com.jaroapps.dailybrain.feature.profile.domain.model.PendingProfileDeletionStatus

enum class PendingProfileDeletionRecoveryAction {
    CLEAN_CLOUD_AND_RETIRE_OLD_AUTH,
    RETIRE_OLD_AUTH_ONLY,
    CONTINUE_WITH_NEW_AUTH,
    BLOCK_UNSAFE_RECREATION,
}

/**
 * Prevents ensureProfile() from running against an old identity while its cloud data is
 * still present. A different UID is accepted only after the old cloud data was deleted.
 */
internal fun resolvePendingProfileDeletionRecoveryAction(
    pending: PendingProfileDeletion,
    signedInFirebaseUid: String,
): PendingProfileDeletionRecoveryAction = when {
    signedInFirebaseUid == pending.firebaseUid &&
        pending.status == PendingProfileDeletionStatus.CLOUD_CLEANUP_PENDING ->
        PendingProfileDeletionRecoveryAction.CLEAN_CLOUD_AND_RETIRE_OLD_AUTH

    signedInFirebaseUid == pending.firebaseUid ->
        PendingProfileDeletionRecoveryAction.RETIRE_OLD_AUTH_ONLY

    pending.status == PendingProfileDeletionStatus.CLOUD_DATA_DELETED ->
        PendingProfileDeletionRecoveryAction.CONTINUE_WITH_NEW_AUTH

    else -> PendingProfileDeletionRecoveryAction.BLOCK_UNSAFE_RECREATION
}
