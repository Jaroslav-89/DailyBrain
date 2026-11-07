package com.jaroapps.dailybrain.feature.shop.domain.model

import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticType
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType

data class ShopCosmeticOwnership(
    val itemId: String,
    val itemType: AvatarCosmeticType,
    val acquiredAtEpochMs: Long,
)

data class ShopReceivedGift(
    val giftId: String,
    val giftType: EndorphinGiftType,
    val senderUserId: String?,
    val receivedAtEpochMs: Long,
    val isOpened: Boolean,
)

sealed interface ShopPurchaseResult {
    data class Purchased(val newBalance: Int) : ShopPurchaseResult
    data class InsufficientFunds(val currentBalance: Int, val requiredAmount: Int) : ShopPurchaseResult
    data class AlreadyProcessed(val currentBalance: Int) : ShopPurchaseResult
    data object AlreadyOwned : ShopPurchaseResult
    data object PatchLimitReached : ShopPurchaseResult
    data object AccountNotLinked : ShopPurchaseResult
    data object InvalidRecipient : ShopPurchaseResult
}


enum class ShopPurchaseItemType {
    PATCH,
    GIFT,
    COSTUME,
    BACKGROUND,
}

/** A locally recorded shop transaction or a gift sent to another profile. */
data class ShopPurchaseHistoryEntry(
    val id: String,
    val itemType: ShopPurchaseItemType,
    val itemId: String,
    val isGift: Boolean,
    val price: Int,
    val occurredAtEpochMs: Long,
    val recipientFirebaseUid: String? = null,
    val recipientName: String? = null,
)
