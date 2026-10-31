package com.jaroapps.dailybrain.feature.profile.data.repository

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.UsernamePolicy
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseSessionMismatchException
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryNotConfiguredException
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryProfileMissingException
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UsernameTakenException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firestore-backed global identity directory:
 * users/{uid} is private, publicProfiles/{uid} is a safe projection,
 * and usernames/{lowercaseHandle} is the unique reservation/lookup index.
 */
class FirebaseUserDirectoryRepository(
    context: Context,
) : UserDirectoryRepository {
    private val appContext = context.applicationContext

    override suspend fun ensureProfile(
        firebaseUid: String,
        email: String?,
        displayName: String?,
        avatarUrl: String?,
    ): String {
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val privateRef = firestore.collection(USERS).document(firebaseUid)
        val publicRef = firestore.collection(PUBLIC_PROFILES).document(firebaseUid)

        return firestore.runTransaction { transaction: Transaction ->
            val privateSnapshot = transaction.get(privateRef)
            val publicSnapshot = transaction.get(publicRef)

            val username = UsernamePolicy.normalize(
                privateSnapshot.getString("usernameLower")
                    ?: privateSnapshot.getString("username")
                    ?: publicSnapshot.getString("usernameLower")
                    ?: publicSnapshot.getString("username").orEmpty(),
            )
            val reservationRef = username.takeIf(String::isNotBlank)
                ?.let { firestore.collection(USERNAMES).document(it) }
            val reservation = reservationRef?.let { transaction.get(it) }
            if (reservation?.exists() == true && reservation.getString("userId") != firebaseUid) {
                throw UsernameTakenException()
            }

            val normalizedEmail = email?.trim().orEmpty()
            val normalizedDisplayName = displayName?.trim().orEmpty()
            val privateFields = mapOf(
                "uid" to firebaseUid,
                "email" to normalizedEmail,
                "emailLower" to normalizedEmail.lowercase(Locale.ROOT),
                "displayName" to normalizedDisplayName,
                "avatarUrl" to avatarUrl.orEmpty(),
                "username" to username,
                "usernameLower" to username,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            val publicFields = mapOf(
                "uid" to firebaseUid,
                "displayName" to normalizedDisplayName,
                "avatarUrl" to avatarUrl.orEmpty(),
                "username" to username,
                "usernameLower" to username,
                "updatedAt" to FieldValue.serverTimestamp(),
            )

            transaction.set(privateRef, privateFields, SetOptions.merge())
            transaction.set(publicRef, publicFields, SetOptions.merge())
            if (reservationRef != null && reservation?.exists() != true) {
                transaction.set(reservationRef, mapOf("userId" to firebaseUid))
            }
            username
        }.awaitResult()
    }

    override suspend fun isUsernameAvailable(firebaseUid: String, username: String): Boolean {
        val normalized = UsernamePolicy.normalize(username)
        require(UsernamePolicy.isValid(normalized)) { "Invalid username." }
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val snapshot = FirebaseFirestore.getInstance(app)
            .collection(USERNAMES)
            .document(normalized)
            .get()
            .awaitResult()
        return !snapshot.exists() || snapshot.getString("userId") == firebaseUid
    }

    override suspend fun updateUsername(firebaseUid: String, username: String) {
        val normalized = UsernamePolicy.normalize(username)
        require(normalized.isEmpty() || UsernamePolicy.isValid(normalized)) { "Invalid username." }
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val privateRef = firestore.collection(USERS).document(firebaseUid)
        val publicRef = firestore.collection(PUBLIC_PROFILES).document(firebaseUid)

        firestore.runTransaction { transaction: Transaction ->
            val privateSnapshot = transaction.get(privateRef)
            val publicSnapshot = transaction.get(publicRef)
            if (!privateSnapshot.exists()) throw UserDirectoryProfileMissingException()

            val previousUsername = UsernamePolicy.normalize(
                privateSnapshot.getString("usernameLower")
                    ?: privateSnapshot.getString("username").orEmpty(),
            )
            val nextReservationRef = normalized.takeIf {
                it.isNotBlank() && it != previousUsername
            }?.let { firestore.collection(USERNAMES).document(it) }
            val nextReservation = nextReservationRef?.let { transaction.get(it) }
            val previousReservationRef = previousUsername.takeIf {
                it.isNotBlank() && it != normalized
            }?.let { firestore.collection(USERNAMES).document(it) }
            val previousReservation = previousReservationRef?.let { transaction.get(it) }

            if (nextReservation?.exists() == true &&
                nextReservation.getString("userId") != firebaseUid
            ) {
                throw UsernameTakenException()
            }

            val displayName = publicSnapshot.getString("displayName")
                ?: privateSnapshot.getString("displayName").orEmpty()
            val avatarUrl = publicSnapshot.getString("avatarUrl")
                ?: privateSnapshot.getString("avatarUrl").orEmpty()
            val now = FieldValue.serverTimestamp()
            transaction.set(
                privateRef,
                mapOf("username" to normalized, "usernameLower" to normalized, "updatedAt" to now),
                SetOptions.merge(),
            )
            transaction.set(
                publicRef,
                mapOf(
                    "uid" to firebaseUid,
                    "displayName" to displayName,
                    "avatarUrl" to avatarUrl,
                    "username" to normalized,
                    "usernameLower" to normalized,
                    "updatedAt" to now,
                ),
                SetOptions.merge(),
            )

            if (nextReservationRef != null && nextReservation?.exists() != true) {
                transaction.set(nextReservationRef, mapOf("userId" to firebaseUid))
            }
            if (previousReservationRef != null &&
                previousReservation?.exists() == true &&
                previousReservation.getString("userId") == firebaseUid
            ) {
                transaction.delete(previousReservationRef)
            }
            true
        }.awaitResult()
    }

    override suspend fun unlinkProfile(firebaseUid: String) {
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val privateRef = firestore.collection(USERS).document(firebaseUid)
        val publicRef = firestore.collection(PUBLIC_PROFILES).document(firebaseUid)

        firestore.runTransaction { transaction: Transaction ->
            // Complete all reads before writes, as required by Firestore transactions.
            val privateSnapshot = transaction.get(privateRef)
            val publicSnapshot = transaction.get(publicRef)
            val username = UsernamePolicy.normalize(
                privateSnapshot.getString("usernameLower")
                    ?: privateSnapshot.getString("username")
                    ?: publicSnapshot.getString("usernameLower")
                    ?: publicSnapshot.getString("username").orEmpty(),
            )
            val reservationRef = username.takeIf(String::isNotBlank)
                ?.let { firestore.collection(USERNAMES).document(it) }
            val reservation = reservationRef?.let { transaction.get(it) }

            if (reservation?.exists() == true && reservation.getString("userId") != firebaseUid) {
                throw IllegalStateException("Username reservation ownership mismatch.")
            }

            if (privateSnapshot.exists()) transaction.delete(privateRef)
            if (publicSnapshot.exists()) transaction.delete(publicRef)
            if (reservation?.exists() == true) {
                transaction.delete(requireNotNull(reservationRef))
            }
            true
        }.awaitResult()
    }

    override suspend fun findUserByUsername(
        firebaseUid: String,
        username: String,
    ): PublicUserProfile? {
        val normalized = UsernamePolicy.normalize(username)
        require(UsernamePolicy.isValid(normalized)) { "Invalid username." }
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val reservation = firestore.collection(USERNAMES)
            .document(normalized)
            .get()
            .awaitResult()
        val targetUid = reservation.getString("userId") ?: return null
        val publicProfile = firestore.collection(PUBLIC_PROFILES)
            .document(targetUid)
            .get()
            .awaitResult()
        if (!publicProfile.exists()) return null

        return PublicUserProfile(
            firebaseUid = targetUid,
            username = publicProfile.getString("username")
                ?: publicProfile.getString("usernameLower")
                ?: normalized,
            displayName = publicProfile.getString("displayName")?.takeIf(String::isNotBlank),
            avatarUrl = publicProfile.getString("avatarUrl")?.takeIf(String::isNotBlank),
        )
    }

    private fun configuredApp(): FirebaseApp =
        FirebaseApp.getApps(appContext).firstOrNull()
            ?: throw UserDirectoryNotConfiguredException()

    private fun requireCurrentUser(app: FirebaseApp, expectedUid: String) {
        val currentUid = FirebaseAuth.getInstance(app).currentUser?.uid
        if (currentUid != expectedUid) throw FirebaseSessionMismatchException()
    }

    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Firestore request failed."),
                    )
                }
            }
        }

    private companion object {
        const val USERS = "users"
        const val PUBLIC_PROFILES = "publicProfiles"
        const val USERNAMES = "usernames"
    }
}
