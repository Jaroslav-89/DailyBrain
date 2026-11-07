package com.jaroapps.dailybrain.feature.shop.domain.repository

interface GiftDeliveryRepository {
    suspend fun syncPendingOutgoing(giftId: String? = null): Boolean
    suspend fun syncIncoming(): Int
}
