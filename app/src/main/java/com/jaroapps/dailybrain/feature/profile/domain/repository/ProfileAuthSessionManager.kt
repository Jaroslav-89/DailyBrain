package com.jaroapps.dailybrain.feature.profile.domain.repository

import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile

/**
 * Re-authenticates Firebase for a profile that already has a Google account binding.
 * Implementations may request interactive Google sign-in when the current session differs.
 */
interface ProfileAuthSessionManager {
    suspend fun reauthenticate(profile: UserProfile)
}

class ProfileGoogleSignInCancelledException : IllegalStateException()
class ProfileGoogleAccountMismatchException : IllegalStateException()
class ProfileGoogleAccountNotLinkedException : IllegalStateException()
class ProfileFirebaseUidMismatchException : IllegalStateException()
class ProfileFirebaseNotConfiguredException : IllegalStateException()
class ProfileFirebaseAuthenticationException(val diagnosticCode: String) : IllegalStateException()
