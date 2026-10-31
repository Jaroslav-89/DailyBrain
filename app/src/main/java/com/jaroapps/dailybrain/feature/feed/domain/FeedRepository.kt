package com.jaroapps.dailybrain.feature.feed.domain

import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun observeFeed(): Flow<List<FeedEvent>>
    suspend fun refresh()
    suspend fun syncOwnEvents()
}

