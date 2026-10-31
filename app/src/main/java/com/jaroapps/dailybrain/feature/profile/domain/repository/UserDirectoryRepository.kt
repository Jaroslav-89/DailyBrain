package com.jaroapps.dailybrain.feature.profile.domain.repository

import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile

class UserDirectoryNotConfiguredException : IllegalStateException("Firebase is not configured.")
class FirebaseSessionMismatchException : IllegalStateException("The active Firebase account does not match the selected local profile.")
class UsernameTakenException : IllegalStateException("The username is already reserved.")
class UserDirectoryProfileMissingException : IllegalStateException("The cloud user profile does not exist.")

interface UserDirectoryRepository {
    suspend fun ensureProfile(firebaseUid: String, email: String?, displayName: String?, avatarUrl: String?): String
    suspend fun isUsernameAvailable(firebaseUid: String, username: String): Boolean
    suspend fun updateUsername(firebaseUid: String, username: String)
    suspend fun unlinkProfile(firebaseUid: String)
    suspend fun findUserByUsername(firebaseUid: String, username: String): PublicUserProfile?
    suspend fun getOrCreateInviteCode(firebaseUid: String): String
    suspend fun findUserByInviteCode(firebaseUid: String, inviteCode: String): PublicUserProfile?
    suspend fun followUser(firebaseUid: String, targetFirebaseUid: String)
    suspend fun isFollowing(firebaseUid: String, targetFirebaseUid: String): Boolean
    suspend fun unfollowUser(firebaseUid: String, targetFirebaseUid: String)
    suspend fun getFollowing(firebaseUid: String): List<PublicUserProfile>
    suspend fun getFollowers(firebaseUid: String): List<PublicUserProfile>
}
