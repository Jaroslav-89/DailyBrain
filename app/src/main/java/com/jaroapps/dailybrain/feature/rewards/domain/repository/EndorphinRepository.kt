package com.jaroapps.dailybrain.feature.rewards.domain.repository

import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinSpendReason
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinSpendResult
import com.jaroapps.dailybrain.feature.rewards.domain.model.GiftReceiptResult

interface EndorphinRepository {
    /**
     * Atomically charges the active profile. transactionId must be stable for retries.
     * A charge is rejected unless the current ledger balance covers the whole amount.
     */
    suspend fun spend(
        amount: Int,
        transactionId: String,
        reason: EndorphinSpendReason,
    ): EndorphinSpendResult

    /**
     * Credits an incoming gift once and stores it in the local gift inventory.
     * Call only after the incoming gift has been verified by the trusted gift-sync flow.
     */
    suspend fun receiveGift(
        giftId: String,
        giftType: EndorphinGiftType,
        senderUserId: String? = null,
        senderName: String? = null,
    ): GiftReceiptResult
}
