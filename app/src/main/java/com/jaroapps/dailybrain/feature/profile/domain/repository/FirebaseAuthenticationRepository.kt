package com.jaroapps.dailybrain.feature.profile.domain.repository

sealed interface FirebaseGoogleSignInResult {
    data class Success(val firebaseUid: String) : FirebaseGoogleSignInResult
    data object NotConfigured : FirebaseGoogleSignInResult
    data object Failed : FirebaseGoogleSignInResult
}

/**
 * Firebase session boundary for Google authentication.
 * Firebase SDK owns and persists its auth session; DailyBrain never stores ID tokens.
 */
interface FirebaseAuthenticationRepository {
    suspend fun signInWithGoogle(
        idToken: String,
        expectedGoogleSubject: String,
    ): FirebaseGoogleSignInResult

    fun signOut()
}
