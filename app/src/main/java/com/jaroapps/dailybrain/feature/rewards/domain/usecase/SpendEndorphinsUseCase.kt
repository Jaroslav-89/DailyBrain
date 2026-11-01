package com.jaroapps.dailybrain.feature.rewards.domain.usecase

import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinSpendReason
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinSpendResult
import com.jaroapps.dailybrain.feature.rewards.domain.repository.EndorphinRepository

class SpendEndorphinsUseCase(
    private val repository: EndorphinRepository,
) {
    suspend operator fun invoke(
        amount: Int,
        transactionId: String,
        reason: EndorphinSpendReason,
    ): EndorphinSpendResult {
        require(amount > 0) { "Endorphin spend amount must be positive." }
        require(transactionId.isNotBlank()) { "A stable transaction ID is required." }
        return repository.spend(
            amount = amount,
            transactionId = transactionId.trim(),
            reason = reason,
        )
    }
}
