package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.model.PendingProfileDeletion
import com.jaroapps.dailybrain.feature.profile.domain.model.PendingProfileDeletionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileDeletionRecoveryPolicyTest {
    private val oldUid = "deleted-firebase-uid"

    @Test
    fun pendingCloudCleanupWithOldUid_requiresCleanupBeforeAuthDeletion() {
        assertEquals(
            PendingProfileDeletionRecoveryAction.CLEAN_CLOUD_AND_RETIRE_OLD_AUTH,
            resolvePendingProfileDeletionRecoveryAction(pending(), oldUid),
        )
    }

    @Test
    fun cloudAlreadyDeletedWithOldUid_onlyRetiresFirebaseAuth() {
        assertEquals(
            PendingProfileDeletionRecoveryAction.RETIRE_OLD_AUTH_ONLY,
            resolvePendingProfileDeletionRecoveryAction(
                pending(PendingProfileDeletionStatus.CLOUD_DATA_DELETED),
                oldUid,
            ),
        )
    }

    @Test
    fun cloudAlreadyDeletedWithNewUid_completesInterruptedRecovery() {
        assertEquals(
            PendingProfileDeletionRecoveryAction.CONTINUE_WITH_NEW_AUTH,
            resolvePendingProfileDeletionRecoveryAction(
                pending(PendingProfileDeletionStatus.CLOUD_DATA_DELETED),
                "new-firebase-uid",
            ),
        )
    }

    @Test
    fun pendingCloudCleanupWithUnexpectedUid_blocksProfileRecreation() {
        assertEquals(
            PendingProfileDeletionRecoveryAction.BLOCK_UNSAFE_RECREATION,
            resolvePendingProfileDeletionRecoveryAction(pending(), "unexpected-firebase-uid"),
        )
    }

    private fun pending(
        status: PendingProfileDeletionStatus = PendingProfileDeletionStatus.CLOUD_CLEANUP_PENDING,
    ) = PendingProfileDeletion(
        firebaseUid = oldUid,
        googleSubject = "google-subject",
        status = status,
    )
}
