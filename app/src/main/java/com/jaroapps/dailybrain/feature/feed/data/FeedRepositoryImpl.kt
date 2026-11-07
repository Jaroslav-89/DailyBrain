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
import com.jaroapps.dailybrain.feature.profile.domain.repository.requireMatchingFirebaseSession
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
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
            database.feedEventDao().observeFeed(localAccountId, FEED_PAGE_SIZE + 1).map { events ->
                events.map { it.toDomain() }
            }
        }

    override suspend fun loadOlderEvents(
        beforeOccurredAtEpochMs: Long,
        beforeEventId: String,
        pageSize: Int,
    ): List<FeedEvent> {
        val localAccountId = activeUserProvider.currentUserId()
        return database.feedEventDao().getOlderFeedEvents(
            localAccountId = localAccountId,
            beforeOccurredAtEpochMs = beforeOccurredAtEpochMs,
            beforeEventId = beforeEventId,
            limit = pageSize + 1,
        ).map { it.toDomain() }
    }

    override suspend fun refresh() {
        val localAccountId = activeUserProvider.currentUserId()
        val profile = database.userProfileDao().getProfile(localAccountId) ?: return

        // Recalculate locally before any network checks; this also runs for unlinked/offline profiles.
        database.withTransaction {
            RewardRecalculator.recalculate(database, localAccountId)
        }

        val firebaseUid = profile.firebaseUid
        if (firebaseUid.isNullOrBlank()) {
            // Keep the last successful friend cache available while cloud features are disconnected.
            return
        }

        val app = requireNotNull(configuredApp()) {
            "FIREBASE_NOT_CONFIGURED: check app/google-services.json and Firebase initialization."
        }
        requireCurrentFirebaseUser(app, firebaseUid)
        val firestore = FirebaseFirestore.getInstance(app)

        // Read the canonical follow graph. publicFollowing is a denormalized projection and
        // may be absent for older follows created before that projection was introduced.
        val followingSnapshot = firestore.collection(FOLLOWS)
            .whereEqualTo("followerUid", firebaseUid)
            .get()
            .awaitResult()
        val followingUids = followingSnapshot.documents
            .mapNotNull { it.getString("followingUid") }
            .filter { it.isNotBlank() && it != firebaseUid }
            .distinct()

        val friendEvents = if (followingUids.isEmpty()) {
            emptyList()
        } else {
            loadFollowedAuthorsEvents(
                firestore = firestore,
                localAccountId = localAccountId,
                followingUids = followingUids,
            )
        }

        // Replace the friend cache only after all per-author queries completed successfully.
        // A failed query leaves the last successfully loaded cache intact.
        database.withTransaction {
            // Re-check after network reads so a profile/session switch cannot cache another session's result.
            requireCurrentFirebaseUser(app, firebaseUid)
            database.feedEventDao().deleteCachedFriendEvents(localAccountId)
            if (friendEvents.isNotEmpty()) database.feedEventDao().upsertAll(friendEvents)
        }

        // Uploading our own publications is independent from reading followed authors.
        // A failed own-event write must not hide friends' events from the feed.
        try {
            syncOwnEvents(localAccountId, firebaseUid, firestore)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.w(LOG_TAG, "Could not sync own feed events; friends feed was refreshed.", error)
        }
    }

    private suspend fun loadFollowedAuthorsEvents(
        firestore: FirebaseFirestore,
        localAccountId: String,
        followingUids: List<String>,
    ): List<FeedEventEntity> = coroutineScope {
        // Query one author at a time so Firestore rules can verify an exact author UID
        // against the canonical follows/{viewerUid_authorUid} relationship document.
        // At most nine independent reads run concurrently; each returns at most ten events.
        followingUids.chunked(MAX_CONCURRENT_AUTHOR_QUERIES).flatMap { authorChunk ->
            authorChunk.map { authorUid ->
                async {
                    firestore.collection(FEED_EVENTS)
                        .whereEqualTo("authorUid", authorUid)
                        .orderBy("occurredAtEpochMs", Query.Direction.DESCENDING)
                        .limit(MAX_PUBLIC_EVENTS.toLong())
                        .get()
                        .awaitResult()
                        .documents
                        .mapNotNull { document ->
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
                                relatedUserFirebaseUid = document.getString("relatedUserFirebaseUid"),
                                relatedUserName = document.getString("relatedUserName"),
                                isLocal = false,
                                isRevoked = false,
                                syncState = SYNCED,
                            )
                        }
                }
            }.awaitAll().flatten()
        }.distinctBy { it.eventId }
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
        val app = requireNotNull(configuredApp()) {
            "FIREBASE_NOT_CONFIGURED: check app/google-services.json and Firebase initialization."
        }
        requireCurrentFirebaseUser(app, firebaseUid)
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
        relatedUserFirebaseUid?.let { put("relatedUserFirebaseUid", it) }
        relatedUserName?.let { put("relatedUserName", it) }
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
        relatedUserFirebaseUid = relatedUserFirebaseUid,
        relatedUserName = relatedUserName,
    )

    private fun configuredApp(): FirebaseApp? =
        FirebaseApp.getApps(appContext).firstOrNull()

    private fun requireCurrentFirebaseUser(app: FirebaseApp, uid: String) {
        requireMatchingFirebaseSession(uid, FirebaseAuth.getInstance(app).currentUser?.uid)
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
        const val FOLLOWS = "follows"
        const val FEED_EVENTS = "feedEvents"
        const val MAX_CONCURRENT_AUTHOR_QUERIES = 9
        const val LOG_TAG = "DailyBrainFeed"
        const val MAX_PUBLIC_EVENTS = 10
        const val FEED_PAGE_SIZE = 30
        const val PENDING_UPSERT = "PENDING_UPSERT"
        const val PENDING_DELETE = "PENDING_DELETE"
        const val LOCAL_ONLY = "LOCAL_ONLY"
        const val SYNCED = "SYNCED"
    }
}
