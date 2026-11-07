package com.jaroapps.dailybrain.feature.profile.data.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseGoogleSignInResult
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileAuthSessionManager
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileFirebaseAuthenticationException
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileFirebaseNotConfiguredException
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileFirebaseUidMismatchException
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileGoogleAccountMismatchException
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileGoogleAccountNotLinkedException
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileGoogleSignInCancelledException
import kotlinx.coroutines.CancellationException

/**
 * Restores a previously linked profile's Firebase session on explicit user request.
 * Google tokens remain in memory and are exchanged with Firebase only after checking the stored subject.
 */
class ActiveProfileAuthCoordinator(
    private val googleSignInProvider: GoogleSignInProvider,
    private val firebaseAuthenticationRepository: FirebaseAuthenticationRepository,
) : ProfileAuthSessionManager {

    override suspend fun reauthenticate(profile: UserProfile) {
        val expectedFirebaseUid = profile.firebaseUid?.takeIf(String::isNotBlank)
            ?: throw ProfileGoogleAccountNotLinkedException()

        if (firebaseAuthenticationRepository.currentUserId() == expectedFirebaseUid) return

        val expectedGoogleSubject = profile.googleSubject?.takeIf(String::isNotBlank)
            ?: throw ProfileGoogleAccountNotLinkedException()

        val googleSignIn = try {
            googleSignInProvider.signIn()
        } catch (cancelled: GetCredentialCancellationException) {
            throw ProfileGoogleSignInCancelledException()
        } catch (cancelled: CancellationException) {
            throw cancelled
        }

        if (googleSignIn.account.subject != expectedGoogleSubject) {
            throw ProfileGoogleAccountMismatchException()
        }

        when (
            val authentication = firebaseAuthenticationRepository.signInWithGoogle(
                idToken = googleSignIn.idToken,
                expectedGoogleSubject = expectedGoogleSubject,
            )
        ) {
            FirebaseGoogleSignInResult.NotConfigured ->
                throw ProfileFirebaseNotConfiguredException()
            is FirebaseGoogleSignInResult.Failed ->
                throw ProfileFirebaseAuthenticationException(authentication.diagnosticCode)
            is FirebaseGoogleSignInResult.Success -> {
                if (authentication.firebaseUid != expectedFirebaseUid) {
                    firebaseAuthenticationRepository.signOut()
                    throw ProfileFirebaseUidMismatchException()
                }
            }
        }
    }
}
