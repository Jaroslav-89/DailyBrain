package com.jaroapps.dailybrain.feature.rewards.domain.usecase

import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import com.jaroapps.dailybrain.feature.rewards.domain.model.GiftReceiptResult
import com.jaroapps.dailybrain.feature.rewards.domain.repository.EndorphinRepository

class ReceiveGiftEndorphinsUseCase(
    private val repository: EndorphinRepository,
) {
    suspend operator fun invoke(
        giftId: String,
        giftType: EndorphinGiftType,
        senderUserId: String? = null,
    ): GiftReceiptResult {
        require(giftId.isNotBlank()) { "A stable gift ID is required." }
        return repository.receiveGift(
            giftId = giftId.trim(),
            giftType = giftType,
            senderUserId = senderUserId?.trim()?.takeIf(String::isNotEmpty),
        )
    }
}
