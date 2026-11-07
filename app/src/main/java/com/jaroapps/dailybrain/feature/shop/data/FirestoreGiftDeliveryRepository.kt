package com.jaroapps.dailybrain.feature.shop.data

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.FeedEventEntity
import com.jaroapps.dailybrain.core.database.entity.UserInventoryEntity
import com.jaroapps.dailybrain.core.database.entity.GiftOutboxEntity
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticCatalog
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticItem
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticType
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
        var feedEventsUpdated = false
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
            // Persist the local feed event before marking the outbox item complete.
            // If the app stops after the Firestore write, the next sync safely retries the
            // same document ID and still creates the sender's feed event.
            val recipientCard = firestore.collection("publicProfileCards")
                .document(gift.recipientFirebaseUid)
                .get()
                .awaitResult()
            val recipientName = recipientCard.getString("displayName")
                ?.takeIf(String::isNotBlank)
                ?: recipientCard.getString("username")?.takeIf(String::isNotBlank)?.let { "@$it" }
            if (addOutgoingFeedEvent(
                    localUserId = localUserId,
                    firebaseUid = firebaseUid,
                    senderName = profile.displayName?.trim()?.takeIf(String::isNotEmpty)
                        ?: profile.generatedAlias,
                    senderUsername = profile.username,
                    recipientName = recipientName,
                    gift = gift,
                )
            ) {
                feedEventsUpdated = true
            }
            database.giftOutboxDao().markSent(localUserId, gift.giftId)
            sentCount++
        }
        if (feedEventsUpdated) feedSyncTrigger.requestSync()
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
            val senderUid = document.getString("senderUid") ?: continue
            val senderName = document.getString("senderName")?.take(80)
            val createdAt = document.getLong("createdAtEpochMs") ?: System.currentTimeMillis()

            val cosmeticGift = decodeCosmeticGift(giftTypeName)
            if (cosmeticGift != null) {
                val (itemType, item) = cosmeticGift
                if (document.getLong("amount")?.toInt() != item.price) continue

                val inserted = database.userInventoryDao().insert(
                    UserInventoryEntity(
                        ownerUserId = localUserId,
                        itemId = item.id,
                        itemType = itemType.name,
                        acquiredAtEpochMs = createdAt,
                    ),
                )
                if (inserted != -1L) creditedCount++

                val receiverProfile = database.userProfileDao().getProfile(localUserId) ?: continue
                val eventId = "cosmetic_gift_received:$giftId"
                val publicSenderName = senderName?.takeIf(String::isNotBlank) ?: "Пользователь DailyBrain"
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
                                eventType = COSMETIC_GIFT_RECEIVED,
                                occurredAtEpochMs = createdAt,
                                periodKey = item.id,
                                exerciseType = itemType.name,
                                valueInt = item.price,
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
                continue
            }

            val giftType = EndorphinGiftType.entries.firstOrNull { it.name == giftTypeName } ?: continue
            if (document.getLong("amount")?.toInt() != giftType.price) continue
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
            val creditedEndorphins = database.rewardLedgerDao()
                .getByKey(localUserId, "gift_received:$giftId")
                ?.deltaEndorphins ?: giftType.recipientEndorphins
            val existingEvent = database.feedEventDao().getEvent(localUserId, eventId)
            val needsFeedSync = existingEvent == null ||
                existingEvent.relatedUserFirebaseUid != senderUid ||
                existingEvent.relatedUserName != publicSenderName ||
                existingEvent.valueInt != creditedEndorphins ||
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
                            valueInt = creditedEndorphins,
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

    private suspend fun addOutgoingFeedEvent(
        localUserId: String,
        firebaseUid: String,
        senderName: String,
        senderUsername: String?,
        recipientName: String?,
        gift: GiftOutboxEntity,
    ): Boolean {
        val regularGift = EndorphinGiftType.entries.firstOrNull { it.name == gift.giftType }
        val cosmeticGift = decodeCosmeticGift(gift.giftType)
        if (regularGift == null && cosmeticGift == null) return false

        val eventId = "gift_sent:${gift.giftId}"
        val existingEvent = database.feedEventDao().getEvent(localUserId, eventId)
        if (existingEvent?.syncState == SYNCED && existingEvent.relatedUserName == recipientName) return false

        val event = if (cosmeticGift != null) {
            FeedEventEntity(
                localAccountId = localUserId,
                eventId = eventId,
                authorFirebaseUid = firebaseUid,
                authorName = senderName,
                authorUsername = senderUsername,
                eventType = COSMETIC_GIFT_SENT,
                occurredAtEpochMs = gift.createdAtEpochMs,
                periodKey = cosmeticGift.second.id,
                exerciseType = cosmeticGift.first.name,
                valueInt = gift.amount,
                relatedUserFirebaseUid = gift.recipientFirebaseUid,
                relatedUserName = recipientName,
                isLocal = true,
                isRevoked = false,
                syncState = PENDING_UPSERT,
            )
        } else {
            FeedEventEntity(
                localAccountId = localUserId,
                eventId = eventId,
                authorFirebaseUid = firebaseUid,
                authorName = senderName,
                authorUsername = senderUsername,
                eventType = GIFT_SENT,
                occurredAtEpochMs = gift.createdAtEpochMs,
                periodKey = requireNotNull(regularGift).name,
                valueInt = gift.amount,
                relatedUserFirebaseUid = gift.recipientFirebaseUid,
                relatedUserName = recipientName,
                isLocal = true,
                isRevoked = false,
                syncState = PENDING_UPSERT,
            )
        }
        database.feedEventDao().upsertAll(listOf(event))
        return true
    }

    private fun decodeCosmeticGift(value: String): Pair<AvatarCosmeticType, AvatarCosmeticItem>? {
        val parts = value.split(":")
        if (parts.size != 3 || parts[0] != COSMETIC_GIFT_PREFIX) return null
        val itemType = AvatarCosmeticType.entries.firstOrNull { it.name == parts[1] } ?: return null
        val item = AvatarCosmeticCatalog.find(itemType, parts[2]) ?: return null
        return itemType to item
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
        const val GIFT_SENT = "GIFT_SENT"
        const val COSMETIC_GIFT_SENT = "COSMETIC_GIFT_SENT"
        const val COSMETIC_GIFT_RECEIVED = "COSMETIC_GIFT_RECEIVED"
        const val COSMETIC_GIFT_PREFIX = "COSMETIC"
        const val PENDING_UPSERT = "PENDING_UPSERT"
        const val SYNCED = "SYNCED"
        const val GIFT_DELIVERIES = "giftDeliveries"
        const val ITEMS = "items"
    }
}
