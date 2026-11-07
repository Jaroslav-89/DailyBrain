package com.jaroapps.dailybrain.feature.shop.domain.repository

import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticType
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopCosmeticOwnership
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopPurchaseResult
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopReceivedGift
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopPurchaseHistoryEntry
import kotlinx.coroutines.flow.Flow

interface ShopRepository {
    fun observeInventory(): Flow<List<ShopCosmeticOwnership>>
    fun observePurchaseHistory(): Flow<List<ShopPurchaseHistoryEntry>>
    fun observeReceivedGifts(): Flow<List<ShopReceivedGift>>
    suspend fun purchasePatch(transactionId: String): ShopPurchaseResult
    suspend fun purchaseCosmetic(itemType: AvatarCosmeticType, itemId: String, transactionId: String): ShopPurchaseResult
    suspend fun purchaseGift(recipientFirebaseUid: String, giftType: EndorphinGiftType, giftId: String): ShopPurchaseResult
    suspend fun purchaseCosmeticGift(
        recipientFirebaseUid: String,
        itemType: AvatarCosmeticType,
        itemId: String,
        giftId: String,
    ): ShopPurchaseResult
    suspend fun markGiftOpened(giftId: String)
}
