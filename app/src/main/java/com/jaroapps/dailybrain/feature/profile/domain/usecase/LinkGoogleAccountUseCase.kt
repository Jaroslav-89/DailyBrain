package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.model.GoogleAccountProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.LinkGoogleAccountResult
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseGoogleSignInResult
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import kotlinx.coroutines.CancellationException

class LinkGoogleAccountUseCase(
    private val repository: UserProfileRepository,
    private val firebaseAuthenticationRepository: FirebaseAuthenticationRepository,
    private val userDirectoryRepository: UserDirectoryRepository,
    private val deleteProfileUseCase: DeleteProfileUseCase,
) {
    suspend operator fun invoke(
        localAccountId: String,
        googleAccount: GoogleAccountProfile,
        idToken: String,
    ): LinkGoogleAccountResult {
        if (repository.isGoogleSubjectLinkedToAnotherProfile(localAccountId, googleAccount.subject)) {
            return LinkGoogleAccountResult.ALREADY_LINKED_TO_ANOTHER_PROFILE
        }

        val authentication = when (
            val result = firebaseAuthenticationRepository.signInWithGoogle(
                idToken = idToken,
                expectedGoogleSubject = googleAccount.subject,
            )
        ) {
            FirebaseGoogleSignInResult.NotConfigured ->
                return LinkGoogleAccountResult.FIREBASE_NOT_CONFIGURED
            is FirebaseGoogleSignInResult.Failed ->
                return LinkGoogleAccountResult.FIREBASE_AUTH_FAILED(result.diagnosticCode)
            is FirebaseGoogleSignInResult.Success -> result
        }

        val firebaseUid = try {
            deleteProfileUseCase.completePendingDeletionAfterGoogleSignIn(
                googleSubject = googleAccount.subject,
                signedInFirebaseUid = authentication.firebaseUid,
                googleIdToken = idToken,
            )
        } catch (cancelled: CancellationException) {
            firebaseAuthenticationRepository.signOut()
            throw cancelled
        } catch (_: Exception) {
            firebaseAuthenticationRepository.signOut()
            return LinkGoogleAccountResult.SAVE_FAILED
        }

        val remoteUsername = try {
            userDirectoryRepository.ensureProfile(
                firebaseUid = firebaseUid,
                email = googleAccount.email,
                displayName = googleAccount.displayName,
                avatarUrl = googleAccount.photoUrl,
            )
        } catch (cancelled: CancellationException) {
            firebaseAuthenticationRepository.signOut()
            throw cancelled
        } catch (_: Exception) {
            firebaseAuthenticationRepository.signOut()
            return LinkGoogleAccountResult.SAVE_FAILED
        }

        return try {
            val linkResult = repository.linkGoogleAccount(
                localAccountId = localAccountId,
                googleAccount = googleAccount,
                firebaseUid = firebaseUid,
                username = remoteUsername.takeIf(String::isNotBlank),
            )
            if (linkResult == LinkGoogleAccountResult.ALREADY_LINKED_TO_ANOTHER_PROFILE) {
                firebaseAuthenticationRepository.signOut()
            }
            linkResult
        } catch (cancelled: CancellationException) {
            firebaseAuthenticationRepository.signOut()
            throw cancelled
        } catch (_: Exception) {
            firebaseAuthenticationRepository.signOut()
            LinkGoogleAccountResult.SAVE_FAILED
        }
    }
}
