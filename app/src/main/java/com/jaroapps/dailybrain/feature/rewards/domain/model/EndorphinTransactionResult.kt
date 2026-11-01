package com.jaroapps.dailybrain.feature.rewards.domain.model

sealed interface EndorphinSpendResult {
    data class Spent(val newBalance: Int) : EndorphinSpendResult

    data class InsufficientFunds(
        val currentBalance: Int,
        val requiredAmount: Int,
    ) : EndorphinSpendResult

    /** Returned when the same idempotency key has already been charged. */
    data class AlreadyProcessed(val currentBalance: Int) : EndorphinSpendResult
}

sealed interface GiftReceiptResult {
    data class Credited(val newBalance: Int) : GiftReceiptResult
    data class AlreadyCredited(val currentBalance: Int) : GiftReceiptResult
}
