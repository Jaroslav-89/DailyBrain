package com.jaroapps.dailybrain.feature.profile.domain.model

/** Basic Google identity fields attached to a single local DailyBrain profile. */
data class GoogleAccountProfile(
    /** Stable OIDC subject (sub), not the email address. */
    val subject: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)

enum class LinkGoogleAccountResult {
    LINKED,
    ALREADY_LINKED_TO_ANOTHER_PROFILE,
}
