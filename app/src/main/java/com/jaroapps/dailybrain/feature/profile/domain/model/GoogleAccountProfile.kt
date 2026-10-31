package com.jaroapps.dailybrain.feature.profile.domain.model

/** Basic Google identity fields attached to a single local DailyBrain profile. */
data class GoogleAccountProfile(
    /** Stable OIDC subject (sub), not the email address. */
    val subject: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)

sealed interface LinkGoogleAccountResult {
    data object LINKED : LinkGoogleAccountResult
    data object ALREADY_LINKED_TO_ANOTHER_PROFILE : LinkGoogleAccountResult
    data object FIREBASE_NOT_CONFIGURED : LinkGoogleAccountResult
    data class FIREBASE_AUTH_FAILED(val diagnosticCode: String) : LinkGoogleAccountResult
    data object SAVE_FAILED : LinkGoogleAccountResult
}
