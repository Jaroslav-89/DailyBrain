package com.jaroapps.dailybrain.feature.rewards.domain.model

/**
 * Single source of truth for endorphin rewards and the future shop price list.
 * The shop UI and purchases are intentionally not part of this implementation yet.
 */
enum class EndorphinGiftType(
    val price: Int,
    val displayName: String,
) {
    CANDY(price = 10, displayName = "Конфета"),
    COFFEE(price = 20, displayName = "Кофе"),
    CROISSANT(price = 30, displayName = "Круассан"),
    HAMBURGER(price = 40, displayName = "Гамбургер"),
    CAKE(price = 50, displayName = "Торт"),
}

enum class EndorphinSpendReason {
    PATCH_PURCHASE,
    GIFT_PURCHASE,
    COSTUME_PURCHASE,
    BACKGROUND_PURCHASE,
}

object EndorphinEconomy {
    const val WEEKLY_STREAK_REWARD = 10
    const val WEEKLY_CHALLENGE_REWARD = 10
    const val ARITHMETIC_RECORD_REWARD = 10
    const val MONTHLY_STREAK_REWARD = 50

    const val PATCH_PRICE = 10
    const val MAX_PATCH_COUNT = 5
    const val COSTUME_PRICE = 100
    const val BACKGROUND_PRICE = 100

    val gifts: List<EndorphinGiftType> = EndorphinGiftType.entries.toList()

    /** A negative balance can exist after reward revocation, but can never fund a purchase. */
    fun canSpend(balance: Int, amount: Int): Boolean =
        amount > 0 && balance >= amount
}
