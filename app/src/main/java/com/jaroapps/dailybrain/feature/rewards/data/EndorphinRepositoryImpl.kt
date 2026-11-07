package com.jaroapps.dailybrain.feature.rewards.data

import androidx.room.withTransaction
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.RewardLedgerEntity
import com.jaroapps.dailybrain.core.database.entity.UserGiftEntity
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinEconomy
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinSpendReason
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinSpendResult
import com.jaroapps.dailybrain.feature.rewards.domain.model.GiftReceiptResult
import com.jaroapps.dailybrain.feature.rewards.domain.repository.EndorphinRepository

class EndorphinRepositoryImpl(
    private val database: AppDatabase,
    private val activeUserProvider: ActiveUserProvider,
) : EndorphinRepository {

    override suspend fun spend(
        amount: Int,
        transactionId: String,
        reason: EndorphinSpendReason,
    ): EndorphinSpendResult {
        require(amount > 0) { "Endorphin spend amount must be positive." }
        require(transactionId.isNotBlank()) { "A stable transaction ID is required." }

        val userId = activeUserProvider.currentUserId()
        return database.withTransaction {
            val ledgerDao = database.rewardLedgerDao()
            val rewardKey = "spend:${transactionId.trim()}"
            val existing = ledgerDao.getByKey(userId, rewardKey)
            val currentBalance = ledgerDao.getEndorphinBalance(userId)

            if (existing != null) {
                check(
                    existing.category == CATEGORY_SPENT &&
                        existing.deltaEndorphins == -amount &&
                        existing.reason == reason.name &&
                        existing.isActive
                ) {
                    "The transaction ID is already used by a different endorphin transaction."
                }
                return@withTransaction EndorphinSpendResult.AlreadyProcessed(currentBalance)
            }

            if (!EndorphinEconomy.canSpend(currentBalance, amount)) {
                return@withTransaction EndorphinSpendResult.InsufficientFunds(
                    currentBalance = currentBalance,
                    requiredAmount = amount,
                )
            }

            val now = System.currentTimeMillis()
            ledgerDao.upsertAll(
                listOf(
                    RewardLedgerEntity(
                        userId = userId,
                        rewardKey = rewardKey,
                        deltaXp = 0,
                        deltaEndorphins = -amount,
                        category = CATEGORY_SPENT,
                        reason = reason.name,
                        occurredAtEpochMs = now,
                        isActive = true,
                    ),
                ),
            )

            val newBalance = ledgerDao.getEndorphinBalance(userId)
            persistBalance(userId, newBalance, now)
            EndorphinSpendResult.Spent(newBalance)
        }
    }

    override suspend fun receiveGift(
        giftId: String,
        giftType: EndorphinGiftType,
        senderUserId: String?,
        senderName: String?,
    ): GiftReceiptResult {
        require(giftId.isNotBlank()) { "A stable gift ID is required." }
        val userId = activeUserProvider.currentUserId()

        return database.withTransaction {
            val ledgerDao = database.rewardLedgerDao()
            val rewardKey = "gift_received:${giftId.trim()}"
            val existing = ledgerDao.getByKey(userId, rewardKey)
            val currentBalance = ledgerDao.getEndorphinBalance(userId)

            if (existing != null) {
                check(
                    existing.category == CATEGORY_GIFT_RECEIVED &&
                        existing.deltaEndorphins == giftType.price &&
                        existing.reason == "GIFT_RECEIVED_${giftType.name}" &&
                        existing.isActive
                ) {
                    "The gift ID is already used by a different endorphin transaction."
                }
                return@withTransaction GiftReceiptResult.AlreadyCredited(currentBalance)
            }

            val now = System.currentTimeMillis()
            ledgerDao.upsertAll(
                listOf(
                    RewardLedgerEntity(
                        userId = userId,
                        rewardKey = rewardKey,
                        deltaXp = 0,
                        deltaEndorphins = giftType.price,
                        category = CATEGORY_GIFT_RECEIVED,
                        reason = "GIFT_RECEIVED_${giftType.name}",
                        occurredAtEpochMs = now,
                        isActive = true,
                    ),
                ),
            )
            database.userGiftDao().upsert(
                UserGiftEntity(
                    ownerUserId = userId,
                    giftId = giftId.trim(),
                    giftType = giftType.name,
                    senderUserId = senderUserId,
                    senderName = senderName,
                    receivedAtEpochMs = now,
                    isOpened = false,
                ),
            )

            val newBalance = ledgerDao.getEndorphinBalance(userId)
            persistBalance(userId, newBalance, now)
            GiftReceiptResult.Credited(newBalance)
        }
    }

    private suspend fun persistBalance(userId: String, balance: Int, now: Long) {
        val profileDao = database.userProfileDao()
        val profile = requireNotNull(profileDao.getProfile(userId)) {
            "Active local profile does not exist."
        }
        profileDao.save(
            profile.copy(
                endorphinBalance = balance,
                updatedAtEpochMs = now,
            ),
        )
    }

    private companion object {
        const val CATEGORY_SPENT = "SPENT"
        const val CATEGORY_GIFT_RECEIVED = "GIFT_RECEIVED"
    }
}
