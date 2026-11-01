package com.jaroapps.dailybrain.feature.shop.data

import androidx.room.withTransaction
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.GiftOutboxEntity
import com.jaroapps.dailybrain.core.database.entity.RewardLedgerEntity
import com.jaroapps.dailybrain.core.database.entity.UserInventoryEntity
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticCatalog
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticType
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinEconomy
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinSpendReason
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopCosmeticOwnership
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopPurchaseResult
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopReceivedGift
import com.jaroapps.dailybrain.feature.shop.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
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
            when (val spend = recordSpend(userId, catalogItem.price, transactionId, reason)) {
                is SpendAttempt.Insufficient -> ShopPurchaseResult.InsufficientFunds(spend.balance, spend.required)
                SpendAttempt.AlreadyProcessed -> ShopPurchaseResult.AlreadyProcessed(database.rewardLedgerDao().getEndorphinBalance(userId))
                is SpendAttempt.Completed -> {
                    val now = System.currentTimeMillis()
                    inventoryDao.insert(UserInventoryEntity(userId, catalogItem.id, itemType.name, now))
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

    override suspend fun markGiftOpened(giftId: String) {
        database.userGiftDao().markOpened(activeUserProvider.currentUserId(), giftId)
    }

    private suspend fun recordSpend(
        userId: String,
        amount: Int,
        transactionId: String,
        reason: EndorphinSpendReason,
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
