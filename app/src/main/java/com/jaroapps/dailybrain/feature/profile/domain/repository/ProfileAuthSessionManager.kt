package com.jaroapps.dailybrain.feature.profile.domain.repository

import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile

/**
 * Ensures Firebase is authenticated as the Google account linked to the target local profile.
 * Implementations may request interactive Google sign-in when the persisted Firebase session differs.
 */
interface ProfileAuthSessionManager {
    suspend fun ensureAuthenticated(profile: UserProfile)
}

class ProfileGoogleSignInCancelledException : IllegalStateException()
class ProfileGoogleAccountMismatchException : IllegalStateException()
class ProfileGoogleAccountNotLinkedException : IllegalStateException()
class ProfileFirebaseUidMismatchException : IllegalStateException()
class ProfileFirebaseNotConfiguredException : IllegalStateException()
class ProfileFirebaseAuthenticationException(val diagnosticCode: String) : IllegalStateException()
