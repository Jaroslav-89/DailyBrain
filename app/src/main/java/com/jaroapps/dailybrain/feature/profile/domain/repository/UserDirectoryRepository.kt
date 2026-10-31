package com.jaroapps.dailybrain.feature.profile.domain.repository

import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile

class UserDirectoryNotConfiguredException : IllegalStateException("Firebase is not configured.")
class FirebaseSessionMismatchException : IllegalStateException("The active Firebase account does not match the selected local profile.")
class UsernameTakenException : IllegalStateException("The username is already reserved.")
class UserDirectoryProfileMissingException : IllegalStateException("The cloud user profile does not exist.")

interface UserDirectoryRepository {
    /** Creates the remote profile on first sign-in or refreshes its public-safe fields. */
    suspend fun ensureProfile(
        firebaseUid: String,
        email: String?,
        displayName: String?,
        avatarUrl: String?,
    ): String

    suspend fun isUsernameAvailable(firebaseUid: String, username: String): Boolean

    /** Atomically updates profile documents and acquires/releases username reservations. */
    suspend fun updateUsername(firebaseUid: String, username: String)

    /** Exact username lookup. The repository never lists the public directory. */
    suspend fun findUserByUsername(firebaseUid: String, username: String): PublicUserProfile?
}
