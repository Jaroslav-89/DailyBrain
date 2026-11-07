package com.jaroapps.dailybrain.feature.profile.data.repository

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import com.jaroapps.dailybrain.feature.profile.domain.model.InviteCodePolicy
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.UsernamePolicy
import com.jaroapps.dailybrain.feature.profile.domain.repository.requireMatchingFirebaseSession
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryNotConfiguredException
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryProfileMissingException
import com.jaroapps.dailybrain.feature.profile.domain.repository.PublicProfileDeletedException
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UsernameTakenException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseUserDirectoryRepository(context: Context) : UserDirectoryRepository {
    private val appContext = context.applicationContext

    override suspend fun ensureProfile(firebaseUid: String, email: String?, displayName: String?, avatarUrl: String?): String {
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val privateRef = firestore.collection(USERS).document(firebaseUid)
        val publicRef = firestore.collection(PUBLIC_PROFILES).document(firebaseUid)
        val publicCardRef = firestore.collection(PUBLIC_PROFILE_CARDS).document(firebaseUid)
        val generatedInviteCode = InviteCodePolicy.generate()

        return firestore.runTransaction { transaction: Transaction ->
            val privateSnapshot = transaction.get(privateRef)
            val publicSnapshot = transaction.get(publicRef)
            val publicCardSnapshot = transaction.get(publicCardRef)
            val username = UsernamePolicy.normalize(
                privateSnapshot.getString("usernameLower") ?: privateSnapshot.getString("username")
                    ?: publicSnapshot.getString("usernameLower") ?: publicSnapshot.getString("username").orEmpty(),
            )
            val inviteCode = InviteCodePolicy.normalize(
                privateSnapshot.getString("inviteCode") ?: publicSnapshot.getString("inviteCode").orEmpty(),
            ).ifBlank { generatedInviteCode }

            val usernameRef = username.takeIf(String::isNotBlank)?.let { firestore.collection(USERNAMES).document(it) }
            val usernameReservation = usernameRef?.let { transaction.get(it) }
            if (usernameReservation?.exists() == true && usernameReservation.getString("userId") != firebaseUid) {
                throw UsernameTakenException()
            }
            val inviteRef = firestore.collection(INVITE_CODES).document(inviteCode)
            val inviteReservation = transaction.get(inviteRef)
            if (inviteReservation.exists() && inviteReservation.getString("userId") != firebaseUid) {
                throw IllegalStateException("Invite code reservation ownership mismatch.")
            }

            val now = FieldValue.serverTimestamp()
            transaction.set(privateRef, mapOf(
                "uid" to firebaseUid,
                "email" to "",
                "emailLower" to "",
                "displayName" to "",
                "avatarUrl" to "",
                "username" to username,
                "usernameLower" to username,
                "inviteCode" to inviteCode,
                "updatedAt" to now,
            ), SetOptions.merge())
            transaction.set(publicRef, mapOf(
                "uid" to firebaseUid,
                "displayName" to "",
                "avatarUrl" to "",
                "username" to username,
                "usernameLower" to username,
                "inviteCode" to inviteCode,
                "updatedAt" to now,
            ), SetOptions.merge())
            transaction.set(publicCardRef, mapOf(
                "uid" to firebaseUid,
                "displayName" to publicCardSnapshot.getString("displayName").orEmpty(),
                "username" to username,
                "usernameLower" to username,
                "avatarConfigurationJson" to publicCardSnapshot.getString("avatarConfigurationJson").orEmpty(),
                "updatedAt" to now,
            ), SetOptions.merge())
            if (usernameRef != null && usernameReservation?.exists() != true) {
                transaction.set(usernameRef, mapOf("userId" to firebaseUid))
            }
            if (!inviteReservation.exists()) transaction.set(inviteRef, mapOf("userId" to firebaseUid))
            username
        }.awaitResult()
    }

    override suspend fun isUsernameAvailable(firebaseUid: String, username: String): Boolean {
        val normalized = UsernamePolicy.normalize(username)
        require(UsernamePolicy.isValid(normalized)) { "Invalid username." }
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val snapshot = FirebaseFirestore.getInstance(app).collection(USERNAMES).document(normalized).get().awaitResult()
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
        val publicCardRef = firestore.collection(PUBLIC_PROFILE_CARDS).document(firebaseUid)

        firestore.runTransaction { transaction: Transaction ->
            val privateSnapshot = transaction.get(privateRef)
            val publicSnapshot = transaction.get(publicRef)
            val publicCardSnapshot = transaction.get(publicCardRef)
            if (!privateSnapshot.exists()) throw UserDirectoryProfileMissingException()
            val previous = UsernamePolicy.normalize(
                privateSnapshot.getString("usernameLower") ?: privateSnapshot.getString("username").orEmpty(),
            )
            val nextRef = normalized.takeIf { it.isNotBlank() && it != previous }
                ?.let { firestore.collection(USERNAMES).document(it) }
            val nextReservation = nextRef?.let { transaction.get(it) }
            val previousRef = previous.takeIf { it.isNotBlank() && it != normalized }
                ?.let { firestore.collection(USERNAMES).document(it) }
            val previousReservation = previousRef?.let { transaction.get(it) }
            if (nextReservation?.exists() == true && nextReservation.getString("userId") != firebaseUid) {
                throw UsernameTakenException()
            }

            val displayName = publicSnapshot.getString("displayName") ?: privateSnapshot.getString("displayName").orEmpty()
            val avatarUrl = publicSnapshot.getString("avatarUrl") ?: privateSnapshot.getString("avatarUrl").orEmpty()
            val inviteCode = privateSnapshot.getString("inviteCode") ?: publicSnapshot.getString("inviteCode").orEmpty()
            val now = FieldValue.serverTimestamp()
            transaction.set(privateRef, mapOf("username" to normalized, "usernameLower" to normalized, "updatedAt" to now), SetOptions.merge())
            transaction.set(publicRef, mapOf(
                "uid" to firebaseUid,
                "displayName" to displayName,
                "avatarUrl" to avatarUrl,
                "username" to normalized,
                "usernameLower" to normalized,
                "inviteCode" to inviteCode,
                "updatedAt" to now,
            ), SetOptions.merge())
            transaction.set(publicCardRef, mapOf(
                "uid" to firebaseUid,
                "displayName" to publicCardSnapshot.getString("displayName").orEmpty(),
                "username" to normalized,
                "usernameLower" to normalized,
                "avatarConfigurationJson" to publicCardSnapshot.getString("avatarConfigurationJson").orEmpty(),
                "updatedAt" to now,
            ), SetOptions.merge())
            if (nextRef != null && nextReservation?.exists() != true) transaction.set(nextRef, mapOf("userId" to firebaseUid))
            if (previousRef != null && previousReservation?.exists() == true &&
                previousReservation.getString("userId") == firebaseUid
            ) transaction.delete(previousRef)
            true
        }.awaitResult()
    }

    override suspend fun getOrCreateInviteCode(firebaseUid: String): String {
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val privateRef = firestore.collection(USERS).document(firebaseUid)
        val publicRef = firestore.collection(PUBLIC_PROFILES).document(firebaseUid)
        val generated = InviteCodePolicy.generate()

        return firestore.runTransaction { transaction ->
            val privateSnapshot = transaction.get(privateRef)
            val publicSnapshot = transaction.get(publicRef)
            if (!privateSnapshot.exists() || !publicSnapshot.exists()) throw UserDirectoryProfileMissingException()
            val existing = InviteCodePolicy.normalize(
                privateSnapshot.getString("inviteCode") ?: publicSnapshot.getString("inviteCode").orEmpty(),
            )
            val code = existing.ifBlank { generated }
            require(InviteCodePolicy.isValid(code)) { "Invalid invite code stored in cloud profile." }
            val reservationRef = firestore.collection(INVITE_CODES).document(code)
            val reservation = transaction.get(reservationRef)
            if (reservation.exists() && reservation.getString("userId") != firebaseUid) {
                throw IllegalStateException("Invite code reservation ownership mismatch.")
            }
            val now = FieldValue.serverTimestamp()
            transaction.set(privateRef, mapOf("inviteCode" to code, "updatedAt" to now), SetOptions.merge())
            transaction.set(publicRef, mapOf("uid" to firebaseUid, "inviteCode" to code, "updatedAt" to now), SetOptions.merge())
            if (!reservation.exists()) transaction.set(reservationRef, mapOf("userId" to firebaseUid))
            code
        }.awaitResult()
    }

    override suspend fun unlinkProfile(firebaseUid: String) {
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val privateRef = firestore.collection(USERS).document(firebaseUid)
        val publicRef = firestore.collection(PUBLIC_PROFILES).document(firebaseUid)
        val publicCardRef = firestore.collection(PUBLIC_PROFILE_CARDS).document(firebaseUid)

        firestore.runTransaction { transaction ->
            val privateSnapshot = transaction.get(privateRef)
            val publicSnapshot = transaction.get(publicRef)
            val publicCardSnapshot = transaction.get(publicCardRef)
            val username = UsernamePolicy.normalize(
                privateSnapshot.getString("usernameLower") ?: privateSnapshot.getString("username")
                    ?: publicSnapshot.getString("usernameLower") ?: publicSnapshot.getString("username").orEmpty(),
            )
            val inviteCode = InviteCodePolicy.normalize(
                privateSnapshot.getString("inviteCode") ?: publicSnapshot.getString("inviteCode").orEmpty(),
            )
            val usernameRef = username.takeIf(String::isNotBlank)?.let { firestore.collection(USERNAMES).document(it) }
            val usernameReservation = usernameRef?.let { transaction.get(it) }
            val inviteRef = inviteCode.takeIf(String::isNotBlank)?.let { firestore.collection(INVITE_CODES).document(it) }
            val inviteReservation = inviteRef?.let { transaction.get(it) }
            if (usernameReservation?.exists() == true && usernameReservation.getString("userId") != firebaseUid) {
                throw IllegalStateException("Username reservation ownership mismatch.")
            }
            if (inviteReservation?.exists() == true && inviteReservation.getString("userId") != firebaseUid) {
                throw IllegalStateException("Invite code reservation ownership mismatch.")
            }
            if (privateSnapshot.exists()) transaction.delete(privateRef)
            if (publicSnapshot.exists()) transaction.delete(publicRef)
            if (publicCardSnapshot.exists()) transaction.delete(publicCardRef)
            if (usernameReservation?.exists() == true) transaction.delete(requireNotNull(usernameRef))
            if (inviteReservation?.exists() == true) transaction.delete(requireNotNull(inviteRef))
            true
        }.awaitResult()

        // Feed events are stored in a top-level collection, so profile unlinking must
        // explicitly remove the author's bounded public history as well.
        firestore.collection("feedEvents")
            .whereEqualTo("authorUid", firebaseUid)
            .limit(10)
            .get()
            .awaitResult()
            .documents
            .forEach { it.reference.delete().awaitResult() }
    }

    /**
     * Erases data owned by a profile. Gifts already delivered to other users are deliberately
     * retained: their balances and received-gift history belong to the recipients.
     */
    override suspend fun deleteProfileData(firebaseUid: String) {
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val deletedProfileRef = firestore.collection(DELETED_PROFILES).document(firebaseUid)
        val deletionMarker = deletedProfileRef.get().awaitResult()
        if (deletionMarker.exists() && deletionMarker.getBoolean("cleanupComplete") == true) return
        if (!deletionMarker.exists()) {
            // Publish the tombstone first so all still-valid sessions for this UID stop
            // creating or mutating records while the client removes the remaining data.
            deletedProfileRef.set(
                mapOf(
                    "uid" to firebaseUid,
                    "deletedAt" to FieldValue.serverTimestamp(),
                    "cleanupComplete" to false,
                ),
            ).awaitResult()
        }

        deleteDocumentsInBatches(
            firestore,
            firestore.collection(PUBLIC_PROFILE_CARDS).document(firebaseUid).collection(COLLECTIBLES),
        )
        deleteDocumentsInBatches(
            firestore,
            firestore.collection(GIFT_DELIVERIES).document(firebaseUid).collection(GIFT_ITEMS),
        )
        deleteFollowRelationships(firestore, firebaseUid, "followerUid")
        deleteFollowRelationships(firestore, firebaseUid, "followingUid")
        deleteDocumentsInBatches(
            firestore,
            firestore.collection(BLOCKED_USERS).document(firebaseUid).collection(PUBLIC_CONNECTIONS),
        )
        deleteOwnFeedEvents(firestore, firebaseUid)

        // Main docs and reservations are removed after their owned subcollections.
        // Sent gifts live under other recipients' paths and are intentionally untouched.
        unlinkProfile(firebaseUid)

        deletedProfileRef.update(
            mapOf(
                "cleanupComplete" to true,
                "cleanedAt" to FieldValue.serverTimestamp(),
            ),
        ).awaitResult()
    }

    private suspend fun deleteDocumentsInBatches(
        firestore: FirebaseFirestore,
        collection: CollectionReference,
    ) {
        while (true) {
            val snapshot = collection.limit(100).get().awaitResult()
            if (snapshot.isEmpty) return
            val batch = firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().awaitResult()
        }
    }

    private suspend fun deleteFollowRelationships(
        firestore: FirebaseFirestore,
        firebaseUid: String,
        participantField: String,
    ) {
        while (true) {
            val snapshot = firestore.collection(FOLLOWS)
                .whereEqualTo(participantField, firebaseUid)
                .limit(80)
                .get()
                .awaitResult()
            if (snapshot.isEmpty) return

            val batch = firestore.batch()
            for (relationship in snapshot.documents) {
                val followerUid = relationship.getString("followerUid")
                val followingUid = relationship.getString("followingUid")
                batch.delete(relationship.reference)
                if (followerUid.isNullOrBlank() || followingUid.isNullOrBlank()) continue

                val followingProjection = firestore.collection(PUBLIC_FOLLOWING).document(followerUid)
                    .collection(PUBLIC_CONNECTIONS).document(followingUid)
                val followerProjection = firestore.collection(PUBLIC_FOLLOWERS).document(followingUid)
                    .collection(PUBLIC_CONNECTIONS).document(followerUid)
                if (followingProjection.get().awaitResult().exists()) batch.delete(followingProjection)
                if (followerProjection.get().awaitResult().exists()) batch.delete(followerProjection)
            }
            batch.commit().awaitResult()
        }
    }

    private suspend fun deleteOwnFeedEvents(firestore: FirebaseFirestore, firebaseUid: String) {
        while (true) {
            val snapshot = firestore.collection(FEED_EVENTS)
                .whereEqualTo("authorUid", firebaseUid)
                .limit(10)
                .get()
                .awaitResult()
            if (snapshot.isEmpty) return
            val batch = firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().awaitResult()
        }
    }

    override suspend fun findUserByUsername(firebaseUid: String, username: String): PublicUserProfile? {
        val normalized = UsernamePolicy.normalize(username)
        require(UsernamePolicy.isValid(normalized)) { "Invalid username." }
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val reservation = firestore.collection(USERNAMES).document(normalized).get().awaitResult()
        val uid = reservation.getString("userId") ?: return null
        return firestore.collection(PUBLIC_PROFILE_CARDS).document(uid).get().awaitResult().toPublicProfile(uid)
    }

    override suspend fun findUserByInviteCode(firebaseUid: String, inviteCode: String): PublicUserProfile? {
        val normalized = InviteCodePolicy.normalize(inviteCode)
        require(InviteCodePolicy.isValid(normalized)) { "Invalid invite code." }
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val reservation = firestore.collection(INVITE_CODES).document(normalized).get().awaitResult()
        val uid = reservation.getString("userId") ?: return null
        return firestore.collection(PUBLIC_PROFILE_CARDS).document(uid).get().awaitResult().toPublicProfile(uid)
    }

    override suspend fun followUser(firebaseUid: String, targetFirebaseUid: String) {
        require(firebaseUid != targetFirebaseUid) { "Cannot follow yourself." }
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val blockedByTarget = isBlockedByProfile(targetFirebaseUid, firebaseUid)
        val blockedByViewer = firestore.collection(BLOCKED_USERS).document(firebaseUid)
            .collection(PUBLIC_CONNECTIONS).document(targetFirebaseUid).get().awaitResult().exists()
        if (blockedByTarget || blockedByViewer) {
            throw IllegalStateException("Подписка недоступна для этого пользователя.")
        }
        val target = firestore.collection(PUBLIC_PROFILES).document(targetFirebaseUid).get().awaitResult()
        if (!target.exists()) throw UserDirectoryProfileMissingException()
        val followRef = firestore.collection(FOLLOWS).document("${firebaseUid}_${targetFirebaseUid}")
        val followingRef = firestore.collection(PUBLIC_FOLLOWING).document(firebaseUid)
            .collection(PUBLIC_CONNECTIONS).document(targetFirebaseUid)
        val followerRef = firestore.collection(PUBLIC_FOLLOWERS).document(targetFirebaseUid)
            .collection(PUBLIC_CONNECTIONS).document(firebaseUid)
        firestore.runTransaction { transaction ->
            val current = transaction.get(followRef)
            val followingProjection = transaction.get(followingRef)
            val followerProjection = transaction.get(followerRef)
            if (!current.exists()) {
                transaction.set(followRef, mapOf(
                    "followerUid" to firebaseUid,
                    "followingUid" to targetFirebaseUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                ))
            }
            if (!followingProjection.exists()) transaction.set(followingRef, mapOf("uid" to targetFirebaseUid))
            if (!followerProjection.exists()) transaction.set(followerRef, mapOf("uid" to firebaseUid))
            true
        }.awaitResult()
    }

    override suspend fun isFollowing(firebaseUid: String, targetFirebaseUid: String): Boolean {
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        return FirebaseFirestore.getInstance(app).collection(FOLLOWS)
            .document("${firebaseUid}_${targetFirebaseUid}").get().awaitResult().exists()
    }

    override suspend fun unfollowUser(firebaseUid: String, targetFirebaseUid: String) {
        require(firebaseUid != targetFirebaseUid) { "Cannot unfollow yourself." }
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val followRef = firestore.collection(FOLLOWS).document("${firebaseUid}_${targetFirebaseUid}")
        val followingRef = firestore.collection(PUBLIC_FOLLOWING).document(firebaseUid)
            .collection(PUBLIC_CONNECTIONS).document(targetFirebaseUid)
        val followerRef = firestore.collection(PUBLIC_FOLLOWERS).document(targetFirebaseUid)
            .collection(PUBLIC_CONNECTIONS).document(firebaseUid)
        firestore.runTransaction { transaction ->
            val current = transaction.get(followRef)
            val followingProjection = transaction.get(followingRef)
            val followerProjection = transaction.get(followerRef)
            if (current.exists()) transaction.delete(followRef)
            if (followingProjection.exists()) transaction.delete(followingRef)
            if (followerProjection.exists()) transaction.delete(followerRef)
            true
        }.awaitResult()
    }

    override suspend fun getFollowing(firebaseUid: String): List<PublicUserProfile> =
        loadConnections(firebaseUid, "followerUid", "followingUid")

    override suspend fun getFollowers(firebaseUid: String): List<PublicUserProfile> =
        loadConnections(firebaseUid, "followingUid", "followerUid")

    private suspend fun loadConnections(uid: String, ownerField: String, relatedField: String): List<PublicUserProfile> {
        val app = configuredApp()
        requireCurrentUser(app, uid)
        val firestore = FirebaseFirestore.getInstance(app)
        val relationships = firestore.collection(FOLLOWS).whereEqualTo(ownerField, uid).get().awaitResult()
        val profiles = mutableListOf<PublicUserProfile>()
        for (relationship in relationships.documents) {
            val relatedUid = relationship.getString(relatedField) ?: continue
            if (isBlockedByProfile(relatedUid, uid)) continue
            if (ownerField == "followerUid") {
                ensurePublicConnection(
                    firestore.collection(PUBLIC_FOLLOWING).document(uid)
                        .collection(PUBLIC_CONNECTIONS).document(relatedUid),
                    relatedUid,
                )
                ensurePublicConnection(
                    firestore.collection(PUBLIC_FOLLOWERS).document(relatedUid)
                        .collection(PUBLIC_CONNECTIONS).document(uid),
                    uid,
                )
            } else {
                ensurePublicConnection(
                    firestore.collection(PUBLIC_FOLLOWERS).document(uid)
                        .collection(PUBLIC_CONNECTIONS).document(relatedUid),
                    relatedUid,
                )
            }
            firestore.collection(PUBLIC_PROFILE_CARDS).document(relatedUid).get().awaitResult()
                .toPublicProfile(relatedUid)?.let(profiles::add)
        }
        return profiles
    }

    private suspend fun ensurePublicConnection(
        reference: com.google.firebase.firestore.DocumentReference,
        relatedUid: String,
    ) {
        if (!reference.get().awaitResult().exists()) {
            reference.set(mapOf("uid" to relatedUid)).awaitResult()
        }
    }

    override suspend fun updatePublicProfile(
        firebaseUid: String,
        displayName: String?,
        country: String?,
        city: String?,
        gender: String?,
        memberSinceEpochMs: Long,
        avatarConfigurationJson: String,
        statistics: com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileStatistics,
    ) {
        val app = configuredApp()
        requireCurrentUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val privateRef = firestore.collection(USERS).document(firebaseUid)
        val publicRef = firestore.collection(PUBLIC_PROFILES).document(firebaseUid)
        val publicCardRef = firestore.collection(PUBLIC_PROFILE_CARDS).document(firebaseUid)
        firestore.runTransaction { transaction ->
            val privateProfile = transaction.get(privateRef)
            val publicProfile = transaction.get(publicRef)
            val publicCard = transaction.get(publicCardRef)
            if (!privateProfile.exists() || !publicProfile.exists()) throw UserDirectoryProfileMissingException()
            val bestResults = statistics.bestExerciseStats.map { result ->
                mapOf(
                    "exerciseType" to result.exerciseType,
                    "bestDurationMs" to result.bestDurationMs,
                    "bestCorrectCount" to result.bestCorrectCount,
                )
            }
            transaction.set(publicRef, mapOf(
                "displayName" to displayName?.trim().orEmpty(),
                "country" to country?.trim().orEmpty(),
                "city" to city?.trim().orEmpty(),
                "gender" to gender.orEmpty(),
                "memberSinceEpochMs" to memberSinceEpochMs,
                "avatarUrl" to "",
                "avatarConfigurationJson" to avatarConfigurationJson,
                "statistics" to mapOf(
                    "currentStreakDays" to statistics.currentStreakDays,
                    "bestStreakDays" to statistics.bestStreakDays,
                    "totalExerciseCount" to statistics.totalExerciseCount,
                    "lastSevenDaysExerciseCount" to statistics.lastSevenDaysExerciseCount,
                    "totalDurationMs" to statistics.totalDurationMs,
                    "firstTrainingDate" to statistics.firstTrainingDate?.toString().orEmpty(),
                    "bestExerciseStats" to bestResults,
                ),
                "updatedAt" to FieldValue.serverTimestamp(),
            ), SetOptions.merge())
            val username = UsernamePolicy.normalize(
                privateProfile.getString("usernameLower") ?: privateProfile.getString("username").orEmpty(),
            )
            transaction.set(publicCardRef, mapOf(
                "uid" to firebaseUid,
                "displayName" to displayName?.trim().orEmpty(),
                "username" to username,
                "usernameLower" to username,
                "avatarConfigurationJson" to avatarConfigurationJson,
                "updatedAt" to FieldValue.serverTimestamp(),
            ), SetOptions.merge())
            true
        }.awaitResult()
    }

    override suspend fun getPublicUserProfile(
        viewerFirebaseUid: String,
        profileFirebaseUid: String,
    ): PublicUserProfile? {
        val app = configuredApp()
        requireCurrentUser(app, viewerFirebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        if (firestore.collection(DELETED_PROFILES).document(profileFirebaseUid).get().awaitResult().exists()) {
            throw PublicProfileDeletedException()
        }
        if (isBlockedByProfile(profileFirebaseUid, viewerFirebaseUid)) {
            return firestore.collection(PUBLIC_PROFILE_CARDS).document(profileFirebaseUid)
                .get().awaitResult().toPublicProfile(profileFirebaseUid)?.copy(isRestricted = true)
        }
        return firestore.collection(PUBLIC_PROFILES).document(profileFirebaseUid)
            .get().awaitResult().toPublicProfile(profileFirebaseUid)
    }

    override suspend fun blockUser(blockerFirebaseUid: String, blockedFirebaseUid: String) {
        require(blockerFirebaseUid != blockedFirebaseUid) { "Cannot block yourself." }
        val app = configuredApp()
        requireCurrentUser(app, blockerFirebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val blockRef = firestore.collection(BLOCKED_USERS).document(blockerFirebaseUid)
            .collection(PUBLIC_CONNECTIONS).document(blockedFirebaseUid)
        val blockerFollowsBlocked = firestore.collection(FOLLOWS).document("${blockerFirebaseUid}_${blockedFirebaseUid}")
        val blockedFollowsBlocker = firestore.collection(FOLLOWS).document("${blockedFirebaseUid}_${blockerFirebaseUid}")
        val blockerFollowing = firestore.collection(PUBLIC_FOLLOWING).document(blockerFirebaseUid)
            .collection(PUBLIC_CONNECTIONS).document(blockedFirebaseUid)
        val blockerFollowers = firestore.collection(PUBLIC_FOLLOWERS).document(blockerFirebaseUid)
            .collection(PUBLIC_CONNECTIONS).document(blockedFirebaseUid)
        val blockedFollowing = firestore.collection(PUBLIC_FOLLOWING).document(blockedFirebaseUid)
            .collection(PUBLIC_CONNECTIONS).document(blockerFirebaseUid)
        val blockedFollowers = firestore.collection(PUBLIC_FOLLOWERS).document(blockedFirebaseUid)
            .collection(PUBLIC_CONNECTIONS).document(blockerFirebaseUid)
        firestore.runTransaction { transaction ->
            val block = transaction.get(blockRef)
            val firstFollow = transaction.get(blockerFollowsBlocked)
            val secondFollow = transaction.get(blockedFollowsBlocker)
            val projection1 = transaction.get(blockerFollowing)
            val projection2 = transaction.get(blockerFollowers)
            val projection3 = transaction.get(blockedFollowing)
            val projection4 = transaction.get(blockedFollowers)
            if (!block.exists()) transaction.set(blockRef, mapOf("uid" to blockedFirebaseUid, "createdAt" to FieldValue.serverTimestamp()))
            if (firstFollow.exists()) transaction.delete(blockerFollowsBlocked)
            if (secondFollow.exists()) transaction.delete(blockedFollowsBlocker)
            if (projection1.exists()) transaction.delete(blockerFollowing)
            if (projection2.exists()) transaction.delete(blockerFollowers)
            if (projection3.exists()) transaction.delete(blockedFollowing)
            if (projection4.exists()) transaction.delete(blockedFollowers)
            true
        }.awaitResult()
    }

    override suspend fun getPublicFollowing(
        viewerFirebaseUid: String,
        profileFirebaseUid: String,
    ): List<PublicUserProfile> = loadPublicConnections(viewerFirebaseUid, profileFirebaseUid, PUBLIC_FOLLOWING)

    override suspend fun getPublicFollowers(
        viewerFirebaseUid: String,
        profileFirebaseUid: String,
    ): List<PublicUserProfile> = loadPublicConnections(viewerFirebaseUid, profileFirebaseUid, PUBLIC_FOLLOWERS)

    override suspend fun isBlockedByProfile(profileFirebaseUid: String, viewerFirebaseUid: String): Boolean {
        val app = configuredApp()
        requireCurrentUser(app, viewerFirebaseUid)
        return FirebaseFirestore.getInstance(app).collection(BLOCKED_USERS).document(profileFirebaseUid)
            .collection(PUBLIC_CONNECTIONS).document(viewerFirebaseUid).get().awaitResult().exists()
    }

    override suspend fun isUserBlocked(blockerFirebaseUid: String, blockedFirebaseUid: String): Boolean {
        val app = configuredApp()
        requireCurrentUser(app, blockerFirebaseUid)
        return FirebaseFirestore.getInstance(app).collection(BLOCKED_USERS).document(blockerFirebaseUid)
            .collection(PUBLIC_CONNECTIONS).document(blockedFirebaseUid).get().awaitResult().exists()
    }

    override suspend fun getBlockedUsers(blockerFirebaseUid: String): List<PublicUserProfile> {
        val app = configuredApp()
        requireCurrentUser(app, blockerFirebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val blocked = firestore.collection(BLOCKED_USERS).document(blockerFirebaseUid)
            .collection(PUBLIC_CONNECTIONS).get().awaitResult()
        val profiles = mutableListOf<PublicUserProfile>()
        for (entry in blocked.documents) {
            firestore.collection(PUBLIC_PROFILE_CARDS).document(entry.id).get().awaitResult()
                .toPublicProfile(entry.id)?.let(profiles::add)
        }
        return profiles
    }

    override suspend fun unblockUser(blockerFirebaseUid: String, blockedFirebaseUid: String) {
        require(blockerFirebaseUid != blockedFirebaseUid) { "Cannot unblock yourself." }
        val app = configuredApp()
        requireCurrentUser(app, blockerFirebaseUid)
        FirebaseFirestore.getInstance(app).collection(BLOCKED_USERS).document(blockerFirebaseUid)
            .collection(PUBLIC_CONNECTIONS).document(blockedFirebaseUid).delete().awaitResult()
    }

    private suspend fun loadPublicConnections(
        viewerFirebaseUid: String,
        profileFirebaseUid: String,
        collectionName: String,
    ): List<PublicUserProfile> {
        val app = configuredApp()
        requireCurrentUser(app, viewerFirebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)
        val connections = firestore.collection(collectionName).document(profileFirebaseUid)
            .collection(PUBLIC_CONNECTIONS).get().awaitResult()
        val profiles = mutableListOf<PublicUserProfile>()
        for (connection in connections.documents) {
            firestore.collection(PUBLIC_PROFILE_CARDS).document(connection.id).get().awaitResult()
                .toPublicProfile(connection.id)?.let(profiles::add)
        }
        return profiles
    }

    private fun DocumentSnapshot.toPublicProfile(uid: String): PublicUserProfile? {
        if (!exists()) return null
        return PublicUserProfile(
            firebaseUid = uid,
            username = getString("username") ?: getString("usernameLower") ?: "",
            displayName = getString("displayName")?.takeIf(String::isNotBlank),
            avatarUrl = getString("avatarUrl")?.takeIf(String::isNotBlank),
            avatarConfigurationJson = getString("avatarConfigurationJson")?.takeIf(String::isNotBlank),
            country = getString("country")?.takeIf(String::isNotBlank),
            city = getString("city")?.takeIf(String::isNotBlank),
            gender = getString("gender")?.takeIf(String::isNotBlank),
            memberSinceEpochMs = getLong("memberSinceEpochMs"),
            statistics = getPublicStatistics(get("statistics") as? Map<*, *>),
        )
    }

    private fun getPublicStatistics(data: Map<*, *>?): com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserStatistics {
        if (data == null) return com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserStatistics()
        val bestResults = (data["bestExerciseStats"] as? List<*>)?.mapNotNull { item ->
            val result = item as? Map<*, *> ?: return@mapNotNull null
            val exerciseType = result["exerciseType"] as? String ?: return@mapNotNull null
            com.jaroapps.dailybrain.feature.profile.domain.model.PublicBestExerciseResult(
                exerciseType = exerciseType,
                bestDurationMs = (result["bestDurationMs"] as? Number)?.toLong(),
                bestCorrectCount = (result["bestCorrectCount"] as? Number)?.toInt(),
            )
        }.orEmpty()
        return com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserStatistics(
            currentStreakDays = (data["currentStreakDays"] as? Number)?.toInt() ?: 0,
            bestStreakDays = (data["bestStreakDays"] as? Number)?.toInt() ?: 0,
            totalExerciseCount = (data["totalExerciseCount"] as? Number)?.toInt() ?: 0,
            lastSevenDaysExerciseCount = (data["lastSevenDaysExerciseCount"] as? Number)?.toInt() ?: 0,
            totalDurationMs = (data["totalDurationMs"] as? Number)?.toLong() ?: 0L,
            firstTrainingDate = (data["firstTrainingDate"] as? String)?.takeIf(String::isNotBlank),
            bestExerciseStats = bestResults,
        )
    }

    private fun configuredApp(): FirebaseApp =
        FirebaseApp.getApps(appContext).firstOrNull() ?: throw UserDirectoryNotConfiguredException()

    private fun requireCurrentUser(app: FirebaseApp, uid: String) {
        requireMatchingFirebaseSession(uid, FirebaseAuth.getInstance(app).currentUser?.uid)
    }

    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                if (task.isSuccessful) continuation.resume(task.result)
                else continuation.resumeWithException(task.exception ?: IllegalStateException("Firestore request failed."))
            }
        }

    private companion object {
        const val USERS = "users"
        const val PUBLIC_PROFILES = "publicProfiles"
        const val PUBLIC_PROFILE_CARDS = "publicProfileCards"
        const val BLOCKED_USERS = "blockedUsers"
        const val USERNAMES = "usernames"
        const val INVITE_CODES = "inviteCodes"
        const val FOLLOWS = "follows"
        const val PUBLIC_FOLLOWING = "publicFollowing"
        const val PUBLIC_FOLLOWERS = "publicFollowers"
        const val PUBLIC_CONNECTIONS = "users"
        const val COLLECTIBLES = "collectibles"
        const val GIFT_DELIVERIES = "giftDeliveries"
        const val GIFT_ITEMS = "items"
        const val FEED_EVENTS = "feedEvents"
        const val DELETED_PROFILES = "deletedProfiles"
    }
}
