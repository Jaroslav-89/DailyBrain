package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.model.GoogleAccountProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.LinkGoogleAccountResult
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseGoogleSignInResult
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import kotlinx.coroutines.CancellationException

class LinkGoogleAccountUseCase(
    private val repository: UserProfileRepository,
    private val firebaseAuthenticationRepository: FirebaseAuthenticationRepository,
) {
    suspend operator fun invoke(
        localAccountId: String,
        googleAccount: GoogleAccountProfile,
        idToken: String,
    ): LinkGoogleAccountResult {
        // Reject a duplicate before changing Firebase's app-wide current-user session.
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
            FirebaseGoogleSignInResult.Failed ->
                return LinkGoogleAccountResult.FIREBASE_AUTH_FAILED
            is FirebaseGoogleSignInResult.Success -> result
        }

        return try {
            val linkResult = repository.linkGoogleAccount(
                localAccountId = localAccountId,
                googleAccount = googleAccount,
                firebaseUid = authentication.firebaseUid,
            )
            if (linkResult == LinkGoogleAccountResult.ALREADY_LINKED_TO_ANOTHER_PROFILE) {
                firebaseAuthenticationRepository.signOut()
            }
            linkResult
        } catch (cancelled: CancellationException) {
            firebaseAuthenticationRepository.signOut()
            throw cancelled
        } catch (_: Exception) {
            // Do not leave a Firebase session active when the local profile binding failed.
            firebaseAuthenticationRepository.signOut()
            LinkGoogleAccountResult.SAVE_FAILED
        }
    }
}
