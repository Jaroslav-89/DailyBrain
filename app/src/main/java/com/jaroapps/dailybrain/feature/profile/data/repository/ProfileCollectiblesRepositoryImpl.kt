package com.jaroapps.dailybrain.feature.profile.data.repository

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.UserBadgeEntity
import com.jaroapps.dailybrain.core.database.entity.UserGiftEntity
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectible
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectibleKind
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseSessionMismatchException
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileCollectiblesRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryNotConfiguredException
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ProfileCollectiblesRepositoryImpl(
    context: Context,
    private val database: AppDatabase,
    private val activeUserProvider: ActiveUserProvider,
) : ProfileCollectiblesRepository {
    private val appContext = context.applicationContext

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeOwn(): Flow<List<ProfileCollectible>> =
        activeUserProvider.observeUserId().flatMapLatest { localUserId ->
            combine(
                database.userGiftDao().observeForUser(localUserId),
                database.userBadgeDao().observeForUser(localUserId),
            ) { gifts, badges ->
                (gifts.map { it.toCollectible() } +
                    badges.filterNot { it.isRevoked }.map { it.toCollectible() })
                    .sortedByDescending(ProfileCollectible::acquiredAtEpochMs)
            }
        }

    override suspend fun getForOwner(ownerFirebaseUid: String): List<ProfileCollectible> {
        require(ownerFirebaseUid.isNotBlank())
        val localUserId = activeUserProvider.currentUserId()
        val profile = database.userProfileDao().getProfile(localUserId)
        if (ownerFirebaseUid == LOCAL_OWNER || profile?.firebaseUid == ownerFirebaseUid) {
            return observeOwn().first()
        }

        val viewerUid = profile?.firebaseUid
            ?: throw IllegalStateException("Подключите Google-аккаунт, чтобы просматривать публичные предметы.")
        val app = configuredApp()
        requireCurrentUser(app, viewerUid)
        val documents = FirebaseFirestore.getInstance(app)
            .collection(PUBLIC_PROFILE_CARDS)
            .document(ownerFirebaseUid)
            .collection(COLLECTIBLES)
            .get()
            .awaitResult()
        return documents.documents.mapNotNull(::decode)
            .sortedByDescending(ProfileCollectible::acquiredAtEpochMs)
    }

    override suspend fun getItem(
        ownerFirebaseUid: String,
        itemId: String,
        kind: ProfileCollectibleKind,
    ): ProfileCollectible? = getForOwner(ownerFirebaseUid).firstOrNull {
        it.id == itemId && it.kind == kind
    }

    override suspend fun syncOwnPublic(ownerFirebaseUid: String, items: List<ProfileCollectible>) {
        val localUserId = activeUserProvider.currentUserId()
        val profile = database.userProfileDao().getProfile(localUserId) ?: return
        if (profile.firebaseUid != ownerFirebaseUid) return
        val app = configuredApp()
        requireCurrentUser(app, ownerFirebaseUid)

        val collection = FirebaseFirestore.getInstance(app)
            .collection(PUBLIC_PROFILE_CARDS)
            .document(ownerFirebaseUid)
            .collection(COLLECTIBLES)
        val existing = collection.get().awaitResult().documents
        val desiredIds = items.mapTo(mutableSetOf()) { documentId(it) }

        items.chunked(MAX_BATCH_WRITES).forEach { chunk ->
            val batch = FirebaseFirestore.getInstance(app).batch()
            chunk.forEach { item ->
                batch.set(collection.document(documentId(item)), item.toPublicMap())
            }
            batch.commit().awaitResult()
        }

        existing.filterNot { it.id in desiredIds }.chunked(MAX_BATCH_WRITES).forEach { stale ->
            val batch = FirebaseFirestore.getInstance(app).batch()
            stale.forEach { batch.delete(it.reference) }
            batch.commit().awaitResult()
        }
    }

    private fun UserGiftEntity.toCollectible() = ProfileCollectible(
        id = giftId,
        kind = ProfileCollectibleKind.GIFT,
        itemType = giftType,
        acquiredAtEpochMs = receivedAtEpochMs,
        senderFirebaseUid = senderUserId,
        senderName = senderName,
        amount = EndorphinGiftType.entries.firstOrNull { it.name == giftType }?.price,
    )

    private fun UserBadgeEntity.toCollectible() = ProfileCollectible(
        id = badgeId,
        kind = ProfileCollectibleKind.BADGE,
        itemType = badgeType,
        acquiredAtEpochMs = awardedAtEpochMs,
        periodKey = periodKey,
        triggerWorkoutId = triggerWorkoutId,
    )

    private fun ProfileCollectible.toPublicMap(): Map<String, Any> = buildMap {
        put("id", id)
        put("kind", kind.name)
        put("itemType", itemType)
        put("acquiredAtEpochMs", acquiredAtEpochMs)
        senderFirebaseUid?.let { put("senderFirebaseUid", it) }
        senderName?.let { put("senderName", it.take(80)) }
        periodKey?.let { put("periodKey", it) }
        triggerWorkoutId?.let { put("triggerWorkoutId", it) }
        amount?.let { put("amount", it) }
    }

    private fun decode(document: DocumentSnapshot): ProfileCollectible? {
        val id = document.getString("id") ?: return null
        val kind = ProfileCollectibleKind.entries.firstOrNull {
            it.name == document.getString("kind")
        } ?: return null
        val itemType = document.getString("itemType") ?: return null
        val acquiredAt = document.getLong("acquiredAtEpochMs") ?: return null
        if (document.id != documentId(kind, id)) return null
        return ProfileCollectible(
            id = id,
            kind = kind,
            itemType = itemType,
            acquiredAtEpochMs = acquiredAt,
            senderFirebaseUid = document.getString("senderFirebaseUid"),
            senderName = document.getString("senderName"),
            periodKey = document.getString("periodKey"),
            triggerWorkoutId = document.getLong("triggerWorkoutId"),
            amount = document.getLong("amount")?.toInt(),
        )
    }

    private fun documentId(item: ProfileCollectible): String = documentId(item.kind, item.id)
    private fun documentId(kind: ProfileCollectibleKind, id: String): String =
        (if (kind == ProfileCollectibleKind.GIFT) "gift_" else "badge_") + id

    private fun configuredApp(): FirebaseApp =
        FirebaseApp.getApps(appContext).firstOrNull() ?: throw UserDirectoryNotConfiguredException()

    private fun requireCurrentUser(app: FirebaseApp, uid: String) {
        if (FirebaseAuth.getInstance(app).currentUser?.uid != uid) throw FirebaseSessionMismatchException()
    }

    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                if (task.isSuccessful) continuation.resume(task.result)
                else continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Не удалось получить коллекционные предметы."),
                )
            }
        }

    private companion object {
        const val LOCAL_OWNER = "local"
        const val PUBLIC_PROFILE_CARDS = "publicProfileCards"
        const val COLLECTIBLES = "collectibles"
        const val MAX_BATCH_WRITES = 400
    }
}
