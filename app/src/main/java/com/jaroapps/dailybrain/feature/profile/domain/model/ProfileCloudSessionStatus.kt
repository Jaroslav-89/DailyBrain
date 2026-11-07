package com.jaroapps.dailybrain.feature.profile.domain.model

/**
 * Availability of cloud features for the currently selected local profile.
 * A stored Google binding is not enough: FirebaseAuth must hold the matching UID.
 */
enum class ProfileCloudSessionStatus {
    NOT_LINKED,
    READY,
    REAUTH_REQUIRED,
}
