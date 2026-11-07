package com.jaroapps.dailybrain.feature.shop.domain.repository

interface GiftDeliveryRepository {
    suspend fun syncPendingOutgoing(): Int
    suspend fun syncIncoming(): Int
}
