package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.model.PendingProfileDeletion
import com.jaroapps.dailybrain.feature.profile.domain.model.PendingProfileDeletionStatus
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Removes local data immediately and completes cloud deletion whenever the old Firebase
 * identity is available. Failed/offline cloud work remains durable in Room for the next
 * explicit Google sign-in.
 */
class DeleteProfileUseCase(
    private val profileRepository: UserProfileRepository,
    private val userDirectoryRepository: UserDirectoryRepository,
    private val firebaseAuthenticationRepository: FirebaseAuthenticationRepository,
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    /**
     * Commits local deletion first and returns immediately. Cloud cleanup runs in an
     * application-owned scope so the profile screen can leave without cancelling it.
     * The Room outbox remains authoritative if the process stops or the old auth session
     * is no longer available.
     */
    suspend operator fun invoke(localAccountId: String) {
        val pending = profileRepository.deleteProfile(localAccountId) ?: return
        val currentFirebaseUid = runCatching { firebaseAuthenticationRepository.currentUserId() }.getOrNull()
        if (currentFirebaseUid != pending.firebaseUid) return

        applicationScope.launch {
            try {
                completeDeletionWithCurrentSession(pending)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Local deletion is already committed. Keep the outbox for the next Google sign-in
                // and prevent the old session from publishing anything in the meantime.
                if (firebaseAuthenticationRepository.currentUserId() == pending.firebaseUid) {
                    runCatching { firebaseAuthenticationRepository.signOut() }
                }
            }
        }
    }

    /**
     * Called after the user explicitly signs into Google but before ensureProfile().
     * If the old Firebase user still exists, clean it, delete it, and exchange the same
     * in-memory Google token again so Firebase creates a fresh UID for the new local profile.
     */
    suspend fun completePendingDeletionAfterGoogleSignIn(
        googleSubject: String,
        signedInFirebaseUid: String,
        googleIdToken: String,
    ): String {
        var firebaseUid = signedInFirebaseUid
        while (true) {
            val pending = profileRepository.findPendingProfileDeletion(
                googleSubject = googleSubject,
                firebaseUid = firebaseUid,
            ) ?: return firebaseUid

            when (resolvePendingProfileDeletionRecoveryAction(pending, firebaseUid)) {
                PendingProfileDeletionRecoveryAction.CLEAN_CLOUD_AND_RETIRE_OLD_AUTH ->
                    cleanCloudDataIfNeeded(pending)
                PendingProfileDeletionRecoveryAction.RETIRE_OLD_AUTH_ONLY -> Unit
                PendingProfileDeletionRecoveryAction.CONTINUE_WITH_NEW_AUTH -> {
                    profileRepository.completePendingProfileDeletion(pending.firebaseUid)
                    continue
                }
                PendingProfileDeletionRecoveryAction.BLOCK_UNSAFE_RECREATION -> throw IllegalStateException(
                    "Cloud cleanup for the deleted Firebase profile has not completed; a new profile cannot be created yet.",
                )
            }

            firebaseAuthenticationRepository.deleteCurrentUser(pending.firebaseUid)
            firebaseAuthenticationRepository.signOut()

            val recreated = firebaseAuthenticationRepository.signInWithGoogle(
                idToken = googleIdToken,
                expectedGoogleSubject = googleSubject,
            )
            firebaseUid = when (recreated) {
                is com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseGoogleSignInResult.Success ->
                    recreated.firebaseUid
                com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseGoogleSignInResult.NotConfigured ->
                    throw IllegalStateException("Firebase is not configured.")
                is com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseGoogleSignInResult.Failed ->
                    throw IllegalStateException("Firebase could not create a new identity: ${recreated.diagnosticCode}")
            }
            check(firebaseUid != pending.firebaseUid) {
                "Firebase returned the deleted UID after account deletion; profile creation was stopped."
            }
            profileRepository.completePendingProfileDeletion(pending.firebaseUid)
        }
    }

    private suspend fun completeDeletionWithCurrentSession(pending: PendingProfileDeletion) {
        cleanCloudDataIfNeeded(pending)
        firebaseAuthenticationRepository.deleteCurrentUser(pending.firebaseUid)
        firebaseAuthenticationRepository.signOut()
        profileRepository.completePendingProfileDeletion(pending.firebaseUid)
    }

    private suspend fun cleanCloudDataIfNeeded(pending: PendingProfileDeletion) {
        if (pending.status == PendingProfileDeletionStatus.CLOUD_CLEANUP_PENDING) {
            userDirectoryRepository.deleteProfileData(pending.firebaseUid)
            profileRepository.markPendingProfileDeletionCloudDataDeleted(pending.firebaseUid)
        }
    }
}
