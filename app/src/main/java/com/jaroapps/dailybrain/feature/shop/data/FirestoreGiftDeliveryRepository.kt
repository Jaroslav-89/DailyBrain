package com.jaroapps.dailybrain.feature.shop.data

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.FeedEventEntity
import com.jaroapps.dailybrain.feature.profile.domain.repository.requireMatchingFirebaseSession
import com.jaroapps.dailybrain.common.domain.service.FeedSyncTrigger
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import com.jaroapps.dailybrain.feature.rewards.domain.model.GiftReceiptResult
import com.jaroapps.dailybrain.feature.rewards.domain.repository.EndorphinRepository
import com.jaroapps.dailybrain.feature.shop.domain.repository.GiftDeliveryRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreGiftDeliveryRepository(
    context: Context,
    private val database: AppDatabase,
    private val activeUserProvider: ActiveUserProvider,
    private val endorphinRepository: EndorphinRepository,
    private val feedSyncTrigger: FeedSyncTrigger,
) : GiftDeliveryRepository {
    private val appContext = context.applicationContext

    override suspend fun syncPendingOutgoing(): Int {
        val localUserId = activeUserProvider.currentUserId()
        val profile = database.userProfileDao().getProfile(localUserId) ?: return 0
        val firebaseUid = profile.firebaseUid ?: return 0
        val app = configuredApp() ?: return 0
        requireCurrentFirebaseUser(app, firebaseUid)

        val firestore = FirebaseFirestore.getInstance(app)
        val pending = database.giftOutboxDao().getPendingForUser(localUserId)
        var sentCount = 0
        for (gift in pending) {
            if (gift.senderFirebaseUid != firebaseUid) continue
            val payload = mapOf(
                "giftId" to gift.giftId,
                "senderUid" to gift.senderFirebaseUid,
                "senderName" to gift.senderName,
                "recipientUid" to gift.recipientFirebaseUid,
                "giftType" to gift.giftType,
                "amount" to gift.amount,
                "createdAtEpochMs" to gift.createdAtEpochMs,
            )
            firestore.collection(GIFT_DELIVERIES)
                .document(gift.recipientFirebaseUid)
                .collection(ITEMS)
                .document(gift.giftId)
                .set(payload)
                .awaitResult()
            database.giftOutboxDao().markSent(localUserId, gift.giftId)
            sentCount++
        }
        return sentCount
    }

    override suspend fun syncIncoming(): Int {
        val localUserId = activeUserProvider.currentUserId()
        val profile = database.userProfileDao().getProfile(localUserId) ?: return 0
        val firebaseUid = profile.firebaseUid ?: return 0
        val app = configuredApp() ?: return 0
        requireCurrentFirebaseUser(app, firebaseUid)

        val snapshot = FirebaseFirestore.getInstance(app)
            .collection(GIFT_DELIVERIES)
            .document(firebaseUid)
            .collection(ITEMS)
            .get()
            .awaitResult()

        var creditedCount = 0
        var feedEventsUpdated = false
        for (document in snapshot.documents) {
            val giftId = document.getString("giftId") ?: document.id
            if (giftId != document.id || document.getString("recipientUid") != firebaseUid) continue
            val giftTypeName = document.getString("giftType") ?: continue
            val giftType = EndorphinGiftType.entries.firstOrNull { it.name == giftTypeName } ?: continue
            if (document.getLong("amount")?.toInt() != giftType.price) continue
            val senderUid = document.getString("senderUid") ?: continue
            val senderName = document.getString("senderName")?.take(80)
            when (endorphinRepository.receiveGift(giftId, giftType, senderUid, senderName)) {
                is GiftReceiptResult.Credited -> creditedCount++
                is GiftReceiptResult.AlreadyCredited -> Unit
            }

            // Backfill metadata on gifts received by older app versions as well.
            val storedGift = database.userGiftDao().getForUser(localUserId, giftId) ?: continue
            val resolvedSenderName = storedGift.senderName ?: senderName
            database.userGiftDao().updateSenderInfo(localUserId, giftId, senderUid, resolvedSenderName)
            val receiverProfile = database.userProfileDao().getProfile(localUserId) ?: continue
            val eventId = "gift_received:$giftId"
            val publicSenderName = resolvedSenderName?.takeIf(String::isNotBlank) ?: "Пользователь DailyBrain"
            val existingEvent = database.feedEventDao().getEvent(localUserId, eventId)
            val needsFeedSync = existingEvent == null ||
                existingEvent.relatedUserFirebaseUid != senderUid ||
                existingEvent.relatedUserName != publicSenderName ||
                existingEvent.syncState != SYNCED
            if (needsFeedSync) {
                database.feedEventDao().upsertAll(
                    listOf(
                        FeedEventEntity(
                            localAccountId = localUserId,
                            eventId = eventId,
                            authorFirebaseUid = firebaseUid,
                            authorName = receiverProfile.displayName?.trim()?.takeIf(String::isNotEmpty)
                                ?: receiverProfile.generatedAlias,
                            authorUsername = receiverProfile.username,
                            eventType = GIFT_RECEIVED,
                            occurredAtEpochMs = storedGift.receivedAtEpochMs,
                            periodKey = giftType.name,
                            valueInt = giftType.price,
                            relatedUserFirebaseUid = senderUid,
                            relatedUserName = publicSenderName,
                            isLocal = true,
                            isRevoked = false,
                            syncState = PENDING_UPSERT,
                        ),
                    ),
                )
                feedEventsUpdated = true
            }
        }
        if (feedEventsUpdated) feedSyncTrigger.requestSync()
        return creditedCount
    }

    private fun configuredApp(): FirebaseApp? = FirebaseApp.getApps(appContext).firstOrNull()

    private fun requireCurrentFirebaseUser(app: FirebaseApp, firebaseUid: String) {
        requireMatchingFirebaseSession(firebaseUid, FirebaseAuth.getInstance(app).currentUser?.uid)
    }

    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                if (task.isSuccessful) continuation.resume(task.result)
                else continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Firestore gift operation failed."),
                )
            }
        }

    private companion object {
        const val GIFT_RECEIVED = "GIFT_RECEIVED"
        const val PENDING_UPSERT = "PENDING_UPSERT"
        const val SYNCED = "SYNCED"
        const val GIFT_DELIVERIES = "giftDeliveries"
        const val ITEMS = "items"
    }
}
