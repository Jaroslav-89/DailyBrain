package com.jaroapps.dailybrain.feature.feed.data

import android.content.Context
import androidx.room.withTransaction
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.FeedEventEntity
import com.jaroapps.dailybrain.feature.feed.domain.FeedEvent
import com.jaroapps.dailybrain.feature.feed.domain.FeedRepository
import com.jaroapps.dailybrain.feature.rewards.data.RewardRecalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FeedRepositoryImpl(
    context: Context,
    private val database: AppDatabase,
    private val activeUserProvider: ActiveUserProvider,
) : FeedRepository {
    private val appContext = context.applicationContext

    override fun observeFeed(): Flow<List<FeedEvent>> =
        activeUserProvider.observeUserId().flatMapLatest { localAccountId ->
            database.feedEventDao().observeFeed(localAccountId).map { events ->
                events.map { it.toDomain() }
            }
        }

    override suspend fun refresh() {
        val localAccountId = activeUserProvider.currentUserId()
        val profile = database.userProfileDao().getProfile(localAccountId) ?: return
        val firebaseUid = profile.firebaseUid
        if (firebaseUid.isNullOrBlank()) {
            database.feedEventDao().deleteCachedFriendEvents(localAccountId)
            return
        }

        val app = configuredApp() ?: return
        requireCurrentFirebaseUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)

        // Refresh event author metadata and ensure newly linked profiles can publish their history.
        database.withTransaction {
            RewardRecalculator.recalculate(database, localAccountId)
        }
        syncOwnEvents(localAccountId, firebaseUid, firestore)

        val followingSnapshot = firestore.collection(PUBLIC_FOLLOWING)
            .document(firebaseUid)
            .collection(CONNECTIONS)
            .get()
            .awaitResult()
        val followingUids = followingSnapshot.documents
            .mapNotNull { it.getString("uid") ?: it.id }
            .filter { it.isNotBlank() && it != firebaseUid }
            .distinct()

        if (followingUids.isEmpty()) {
            database.feedEventDao().deleteCachedFriendEvents(localAccountId)
            return
        }

        // Retention is 20 documents per author. A chunk query can therefore retrieve every
        // retained event for all authors in that chunk without issuing one request per author.
        val friendEvents = followingUids.chunked(MAX_AUTHORS_PER_QUERY).flatMap { authorIds ->
            firestore.collection(FEED_EVENTS)
                .whereIn("authorUid", authorIds)
                .orderBy("occurredAtEpochMs", Query.Direction.DESCENDING)
                .limit((MAX_PUBLIC_EVENTS * authorIds.size).toLong())
                .get()
                .awaitResult()
                .documents
                .mapNotNull { document ->
                    val authorUid = document.getString("authorUid") ?: return@mapNotNull null
                    if (authorUid !in followingUids) return@mapNotNull null
                    val eventType = document.getString("eventType") ?: return@mapNotNull null
                    val occurredAt = document.getLong("occurredAtEpochMs") ?: return@mapNotNull null
                    FeedEventEntity(
                        localAccountId = localAccountId,
                        eventId = document.id,
                        authorFirebaseUid = authorUid,
                        authorName = document.getString("authorName")?.takeIf(String::isNotBlank)
                            ?: "Пользователь",
                        authorUsername = document.getString("authorUsername"),
                        eventType = eventType,
                        occurredAtEpochMs = occurredAt,
                        periodKey = document.getString("periodKey"),
                        exerciseType = document.getString("exerciseType"),
                        valueLong = document.getLong("valueLong"),
                        valueInt = document.getLong("valueInt")?.toInt(),
                        accuracyPercent = document.getDouble("accuracyPercent"),
                        isLocal = false,
                        isRevoked = false,
                        syncState = SYNCED,
                    )
                }
        }

        // Only replace the friend cache after every Firestore query completed successfully.
        // If network access fails, Room keeps showing the last successfully loaded friend feed.
        database.withTransaction {
            database.feedEventDao().deleteCachedFriendEvents(localAccountId)
            if (friendEvents.isNotEmpty()) database.feedEventDao().upsertAll(friendEvents)
        }
    }

    override suspend fun syncOwnEvents() {
        val localAccountId = activeUserProvider.currentUserId()
        val profile = database.userProfileDao().getProfile(localAccountId) ?: return
        val firebaseUid = profile.firebaseUid ?: return
        val app = configuredApp() ?: return
        requireCurrentFirebaseUser(app, firebaseUid)
        syncOwnEvents(localAccountId, firebaseUid, FirebaseFirestore.getInstance(app))
    }

    private suspend fun syncOwnEvents(
        localAccountId: String,
        firebaseUid: String,
        firestore: FirebaseFirestore,
    ) {
        val dao = database.feedEventDao()
        val localEvents = dao.getLocalEvents(localAccountId)

        localEvents.filter {
            it.authorFirebaseUid == firebaseUid && it.isRevoked && it.syncState == PENDING_DELETE
        }.forEach { event ->
            firestore.collection(FEED_EVENTS).document(event.eventId).delete().awaitResult()
            dao.markSynced(localAccountId, event.eventId)
        }

        val latestPublicEvents = localEvents
            .filter { it.authorFirebaseUid == firebaseUid && !it.isRevoked }
            .sortedByDescending { it.occurredAtEpochMs }
            .take(MAX_PUBLIC_EVENTS)
        val publishableIds = latestPublicEvents.map { it.eventId }.toSet()

        // Events older than the public retention window remain in the local history only.
        // Events that were previously published but fell outside the local retention window
        // can be deleted by their stable document IDs without an extra Firestore query.
        localEvents.filter {
            it.authorFirebaseUid == firebaseUid &&
                !it.isRevoked &&
                it.syncState == SYNCED &&
                it.eventId !in publishableIds
        }.forEach { event ->
            firestore.collection(FEED_EVENTS).document(event.eventId).delete().awaitResult()
            dao.updateSyncState(localAccountId, event.eventId, LOCAL_ONLY)
        }

        localEvents.filter {
            it.authorFirebaseUid == firebaseUid &&
                !it.isRevoked &&
                it.syncState == PENDING_UPSERT &&
                it.eventId !in publishableIds
        }.forEach { dao.updateSyncState(localAccountId, it.eventId, LOCAL_ONLY) }

        latestPublicEvents.filter { it.syncState == PENDING_UPSERT }.forEach { event ->
            firestore.collection(FEED_EVENTS)
                .document(event.eventId)
                .set(event.toCloudMap(firebaseUid))
                .awaitResult()
            dao.markSynced(localAccountId, event.eventId)
        }
    }

    private fun FeedEventEntity.toCloudMap(firebaseUid: String): Map<String, Any> = buildMap {
        put("authorUid", firebaseUid)
        put("authorName", authorName)
        authorUsername?.let { put("authorUsername", it) }
        put("eventType", eventType)
        put("occurredAtEpochMs", occurredAtEpochMs)
        periodKey?.let { put("periodKey", it) }
        exerciseType?.let { put("exerciseType", it) }
        valueLong?.let { put("valueLong", it) }
        valueInt?.let { put("valueInt", it) }
        accuracyPercent?.let { put("accuracyPercent", it) }
    }

    private fun FeedEventEntity.toDomain() = FeedEvent(
        eventId = eventId,
        isOwn = isLocal,
        authorFirebaseUid = authorFirebaseUid,
        authorName = authorName,
        authorUsername = authorUsername,
        eventType = eventType,
        occurredAtEpochMs = occurredAtEpochMs,
        periodKey = periodKey,
        exerciseType = exerciseType,
        valueLong = valueLong,
        valueInt = valueInt,
        accuracyPercent = accuracyPercent,
    )

    private fun configuredApp(): FirebaseApp? =
        FirebaseApp.getApps(appContext).firstOrNull()

    private fun requireCurrentFirebaseUser(app: FirebaseApp, uid: String) {
        check(FirebaseAuth.getInstance(app).currentUser?.uid == uid) {
            "The active Firebase account does not match the selected local profile."
        }
    }

    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Firebase operation failed."),
                    )
                }
            }
        }

    private companion object {
        const val PUBLIC_FOLLOWING = "publicFollowing"
        const val CONNECTIONS = "users"
        const val FEED_EVENTS = "feedEvents"
        const val MAX_AUTHORS_PER_QUERY = 9
        const val MAX_PUBLIC_EVENTS = 20
        const val PENDING_UPSERT = "PENDING_UPSERT"
        const val PENDING_DELETE = "PENDING_DELETE"
        const val LOCAL_ONLY = "LOCAL_ONLY"
        const val SYNCED = "SYNCED"
    }
}
