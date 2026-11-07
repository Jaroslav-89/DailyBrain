package com.jaroapps.dailybrain.feature.shop.data

import androidx.room.withTransaction
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.FeedEventEntity
import com.jaroapps.dailybrain.core.database.entity.GiftOutboxEntity
import com.jaroapps.dailybrain.core.database.entity.RewardLedgerEntity
import com.jaroapps.dailybrain.core.database.entity.UserInventoryEntity
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticCatalog
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticType
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinEconomy
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinSpendReason
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopCosmeticOwnership
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopPurchaseHistoryEntry
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopPurchaseItemType
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopPurchaseResult
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopReceivedGift
import com.jaroapps.dailybrain.feature.shop.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class ShopRepositoryImpl(
    private val database: AppDatabase,
    private val activeUserProvider: ActiveUserProvider,
) : ShopRepository {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeInventory(): Flow<List<ShopCosmeticOwnership>> =
        activeUserProvider.observeUserId().flatMapLatest { userId ->
            database.userInventoryDao().observeForUser(userId).map { items ->
                items.mapNotNull { item ->
                    val type = runCatching { AvatarCosmeticType.valueOf(item.itemType) }.getOrNull()
                        ?: return@mapNotNull null
                    ShopCosmeticOwnership(item.itemId, type, item.acquiredAtEpochMs)
                }
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeReceivedGifts(): Flow<List<ShopReceivedGift>> =
        activeUserProvider.observeUserId().flatMapLatest { userId ->
            database.userGiftDao().observeForUser(userId).map { gifts ->
                gifts.mapNotNull { gift ->
                    val type = EndorphinGiftType.entries.firstOrNull { it.name == gift.giftType }
                        ?: return@mapNotNull null
                    ShopReceivedGift(gift.giftId, type, gift.senderUserId, gift.receivedAtEpochMs, gift.isOpened)
                }
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observePurchaseHistory(): Flow<List<ShopPurchaseHistoryEntry>> =
        activeUserProvider.observeUserId().flatMapLatest { userId ->
            combine(
                database.rewardLedgerDao().observeForUser(userId),
                database.giftOutboxDao().observeForUser(userId),
            ) { ledger, outgoingGifts ->
                val directPurchases = ledger.asSequence()
                    .filter { it.isActive && it.deltaEndorphins < 0 }
                    .mapNotNull { operation ->
                        val itemType = when (operation.reason) {
                            EndorphinSpendReason.PATCH_PURCHASE.name -> ShopPurchaseItemType.PATCH
                            EndorphinSpendReason.COSTUME_PURCHASE.name -> ShopPurchaseItemType.COSTUME
                            EndorphinSpendReason.BACKGROUND_PURCHASE.name -> ShopPurchaseItemType.BACKGROUND
                            else -> null
                        } ?: return@mapNotNull null
                        val itemId = when (itemType) {
                            ShopPurchaseItemType.PATCH -> "patch"
                            ShopPurchaseItemType.COSTUME,
                            ShopPurchaseItemType.BACKGROUND -> operation.periodKey ?: return@mapNotNull null
                            ShopPurchaseItemType.GIFT -> return@mapNotNull null
                        }
                        ShopPurchaseHistoryEntry(
                            id = operation.rewardKey,
                            itemType = itemType,
                            itemId = itemId,
                            isGift = false,
                            price = -operation.deltaEndorphins,
                            occurredAtEpochMs = operation.occurredAtEpochMs,
                        )
                    }
                    .toList()

                val sentGifts = outgoingGifts.mapNotNull { gift ->
                    val encodedType = gift.giftType.split(":")
                    val itemType = when {
                        gift.giftType.startsWith("COSMETIC:COSTUME:") -> ShopPurchaseItemType.COSTUME
                        gift.giftType.startsWith("COSMETIC:BACKGROUND:") -> ShopPurchaseItemType.BACKGROUND
                        EndorphinGiftType.entries.any { it.name == gift.giftType } -> ShopPurchaseItemType.GIFT
                        else -> null
                    } ?: return@mapNotNull null
                    val itemId = when (itemType) {
                        ShopPurchaseItemType.COSTUME,
                        ShopPurchaseItemType.BACKGROUND -> encodedType.getOrNull(2) ?: return@mapNotNull null
                        ShopPurchaseItemType.GIFT -> gift.giftType
                        ShopPurchaseItemType.PATCH -> return@mapNotNull null
                    }
                    ShopPurchaseHistoryEntry(
                        id = "gift:${gift.giftId}",
                        itemType = itemType,
                        itemId = itemId,
                        isGift = true,
                        price = gift.amount,
                        occurredAtEpochMs = gift.createdAtEpochMs,
                        recipientFirebaseUid = gift.recipientFirebaseUid,
                    )
                }
                (directPurchases + sentGifts).sortedByDescending(ShopPurchaseHistoryEntry::occurredAtEpochMs)
            }
        }

    override suspend fun purchasePatch(transactionId: String): ShopPurchaseResult {
        require(transactionId.isNotBlank())
        val userId = activeUserProvider.currentUserId()
        return database.withTransaction {
            val profileDao = database.userProfileDao()
            val profile = requireNotNull(profileDao.getProfile(userId)) { "Active profile does not exist." }
            if (profile.patchCount >= EndorphinEconomy.MAX_PATCH_COUNT) {
                return@withTransaction ShopPurchaseResult.PatchLimitReached
            }
            when (val spend = recordSpend(userId, EndorphinEconomy.PATCH_PRICE, transactionId, EndorphinSpendReason.PATCH_PURCHASE)) {
                is SpendAttempt.Insufficient -> ShopPurchaseResult.InsufficientFunds(spend.balance, spend.required)
                SpendAttempt.AlreadyProcessed -> ShopPurchaseResult.AlreadyProcessed(database.rewardLedgerDao().getEndorphinBalance(userId))
                is SpendAttempt.Completed -> {
                    profileDao.save(profile.copy(
                        patchCount = profile.patchCount + 1,
                        endorphinBalance = spend.balance,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    ))
                    ShopPurchaseResult.Purchased(spend.balance)
                }
            }
        }
    }

    override suspend fun purchaseCosmetic(
        itemType: AvatarCosmeticType,
        itemId: String,
        transactionId: String,
    ): ShopPurchaseResult {
        require(transactionId.isNotBlank())
        val catalogItem = AvatarCosmeticCatalog.find(itemType, itemId)
            ?: throw IllegalArgumentException("Unknown cosmetic item: " + itemId)
        val userId = activeUserProvider.currentUserId()
        return database.withTransaction {
            val inventoryDao = database.userInventoryDao()
            if (inventoryDao.ownsItem(userId, catalogItem.id, itemType.name)) {
                return@withTransaction ShopPurchaseResult.AlreadyOwned
            }
            val profileDao = database.userProfileDao()
            val profile = requireNotNull(profileDao.getProfile(userId)) { "Active profile does not exist." }
            val reason = if (itemType == AvatarCosmeticType.COSTUME) {
                EndorphinSpendReason.COSTUME_PURCHASE
            } else {
                EndorphinSpendReason.BACKGROUND_PURCHASE
            }
            when (val spend = recordSpend(
                userId,
                catalogItem.price,
                transactionId,
                reason,
                periodKey = catalogItem.id,
            )) {
                is SpendAttempt.Insufficient -> ShopPurchaseResult.InsufficientFunds(spend.balance, spend.required)
                SpendAttempt.AlreadyProcessed -> ShopPurchaseResult.AlreadyProcessed(database.rewardLedgerDao().getEndorphinBalance(userId))
                is SpendAttempt.Completed -> {
                    val now = System.currentTimeMillis()
                    inventoryDao.insert(UserInventoryEntity(userId, catalogItem.id, itemType.name, now))
                    database.feedEventDao().upsertAll(
                        listOf(
                            FeedEventEntity(
                                localAccountId = userId,
                                eventId = "cosmetic_purchase:$transactionId",
                                authorFirebaseUid = profile.firebaseUid,
                                authorName = profile.displayName?.trim()?.takeIf(String::isNotEmpty)
                                    ?: profile.generatedAlias,
                                authorUsername = profile.username,
                                eventType = "COSMETIC_PURCHASED",
                                occurredAtEpochMs = now,
                                periodKey = catalogItem.id,
                                exerciseType = itemType.name,
                                valueInt = catalogItem.price,
                                isLocal = true,
                                isRevoked = false,
                                syncState = if (profile.firebaseUid.isNullOrBlank()) "LOCAL_ONLY" else "PENDING_UPSERT",
                            ),
                        ),
                    )
                    profileDao.save(profile.copy(endorphinBalance = spend.balance, updatedAtEpochMs = now))
                    ShopPurchaseResult.Purchased(spend.balance)
                }
            }
        }
    }

    override suspend fun purchaseGift(
        recipientFirebaseUid: String,
        giftType: EndorphinGiftType,
        giftId: String,
    ): ShopPurchaseResult {
        require(giftId.isNotBlank())
        require(recipientFirebaseUid.isNotBlank())
        val userId = activeUserProvider.currentUserId()
        return database.withTransaction {
            val profileDao = database.userProfileDao()
            val profile = requireNotNull(profileDao.getProfile(userId)) { "Active profile does not exist." }
            val senderUid = profile.firebaseUid ?: return@withTransaction ShopPurchaseResult.AccountNotLinked
            if (recipientFirebaseUid == senderUid) return@withTransaction ShopPurchaseResult.InvalidRecipient
            val outboxDao = database.giftOutboxDao()
            if (outboxDao.exists(userId, giftId)) {
                return@withTransaction ShopPurchaseResult.AlreadyProcessed(database.rewardLedgerDao().getEndorphinBalance(userId))
            }
            when (val spend = recordSpend(
                userId,
                giftType.price,
                "gift:" + giftId,
                EndorphinSpendReason.GIFT_PURCHASE,
            )) {
                is SpendAttempt.Insufficient -> ShopPurchaseResult.InsufficientFunds(spend.balance, spend.required)
                SpendAttempt.AlreadyProcessed -> ShopPurchaseResult.AlreadyProcessed(database.rewardLedgerDao().getEndorphinBalance(userId))
                is SpendAttempt.Completed -> {
                    val now = System.currentTimeMillis()
                    outboxDao.insert(
                        GiftOutboxEntity(
                            ownerUserId = userId,
                            giftId = giftId,
                            senderFirebaseUid = senderUid,
                            recipientFirebaseUid = recipientFirebaseUid,
                            senderName = (profile.displayName?.trim()?.takeIf(String::isNotEmpty) ?: profile.generatedAlias).take(80),
                            giftType = giftType.name,
                            amount = giftType.price,
                            createdAtEpochMs = now,
                            isSent = false,
                        ),
                    )
                    profileDao.save(profile.copy(endorphinBalance = spend.balance, updatedAtEpochMs = now))
                    ShopPurchaseResult.Purchased(spend.balance)
                }
            }
        }
    }

    override suspend fun purchaseCosmeticGift(
        recipientFirebaseUid: String,
        itemType: AvatarCosmeticType,
        itemId: String,
        giftId: String,
    ): ShopPurchaseResult {
        require(giftId.isNotBlank())
        require(recipientFirebaseUid.isNotBlank())
        val catalogItem = AvatarCosmeticCatalog.find(itemType, itemId)
            ?: throw IllegalArgumentException("Unknown cosmetic item: " + itemId)
        val userId = activeUserProvider.currentUserId()
        return database.withTransaction {
            val profileDao = database.userProfileDao()
            val profile = requireNotNull(profileDao.getProfile(userId)) { "Active profile does not exist." }
            val senderUid = profile.firebaseUid ?: return@withTransaction ShopPurchaseResult.AccountNotLinked
            if (recipientFirebaseUid == senderUid) {
                return@withTransaction ShopPurchaseResult.InvalidRecipient
            }

            val outboxDao = database.giftOutboxDao()
            if (outboxDao.exists(userId, giftId)) {
                return@withTransaction ShopPurchaseResult.AlreadyProcessed(
                    database.rewardLedgerDao().getEndorphinBalance(userId),
                )
            }

            when (val spend = recordSpend(
                userId = userId,
                amount = catalogItem.price,
                transactionId = "gift:" + giftId,
                reason = EndorphinSpendReason.GIFT_PURCHASE,
            )) {
                is SpendAttempt.Insufficient ->
                    ShopPurchaseResult.InsufficientFunds(spend.balance, spend.required)
                SpendAttempt.AlreadyProcessed ->
                    ShopPurchaseResult.AlreadyProcessed(database.rewardLedgerDao().getEndorphinBalance(userId))
                is SpendAttempt.Completed -> {
                    val now = System.currentTimeMillis()
                    outboxDao.insert(
                        GiftOutboxEntity(
                            ownerUserId = userId,
                            giftId = giftId,
                            senderFirebaseUid = senderUid,
                            recipientFirebaseUid = recipientFirebaseUid,
                            senderName = (
                                profile.displayName?.trim()?.takeIf(String::isNotEmpty)
                                    ?: profile.generatedAlias
                                ).take(80),
                            giftType = "COSMETIC:${itemType.name}:${catalogItem.id}",
                            amount = catalogItem.price,
                            createdAtEpochMs = now,
                            isSent = false,
                        ),
                    )
                    profileDao.save(profile.copy(endorphinBalance = spend.balance, updatedAtEpochMs = now))
                    ShopPurchaseResult.Purchased(spend.balance)
                }
            }
        }
    }

    override suspend fun markGiftOpened(giftId: String) {
        database.userGiftDao().markOpened(activeUserProvider.currentUserId(), giftId)
    }

    private suspend fun recordSpend(
        userId: String,
        amount: Int,
        transactionId: String,
        reason: EndorphinSpendReason,
        periodKey: String? = null,
    ): SpendAttempt {
        val ledgerDao = database.rewardLedgerDao()
        val rewardKey = "spend:" + transactionId.trim()
        val existing = ledgerDao.getByKey(userId, rewardKey)
        if (existing != null) {
            check(
                existing.category == CATEGORY_SPENT &&
                    existing.deltaEndorphins == -amount &&
                    existing.reason == reason.name &&
                    existing.isActive
            ) { "The transaction ID is already used by a different endorphin transaction." }
            return SpendAttempt.AlreadyProcessed
        }

        val balance = ledgerDao.getEndorphinBalance(userId)
        if (!EndorphinEconomy.canSpend(balance, amount)) return SpendAttempt.Insufficient(balance, amount)

        ledgerDao.upsertAll(
            listOf(
                RewardLedgerEntity(
                    userId = userId,
                    rewardKey = rewardKey,
                    deltaXp = 0,
                    deltaEndorphins = -amount,
                    category = CATEGORY_SPENT,
                    reason = reason.name,
                    periodKey = periodKey,
                    occurredAtEpochMs = System.currentTimeMillis(),
                    isActive = true,
                ),
            ),
        )
        return SpendAttempt.Completed(ledgerDao.getEndorphinBalance(userId))
    }

    private sealed interface SpendAttempt {
        data class Completed(val balance: Int) : SpendAttempt
        data class Insufficient(val balance: Int, val required: Int) : SpendAttempt
        data object AlreadyProcessed : SpendAttempt
    }

    private companion object {
        const val CATEGORY_SPENT = "SPENT"
    }
}
