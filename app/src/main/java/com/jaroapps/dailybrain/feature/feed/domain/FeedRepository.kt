package com.jaroapps.dailybrain.feature.feed.domain

import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    /** Emits the newest 31 cached events so the UI can display 30 and detect another page. */
    fun observeFeed(): Flow<List<FeedEvent>>
    suspend fun loadOlderEvents(
        beforeOccurredAtEpochMs: Long,
        beforeEventId: String,
        pageSize: Int,
    ): List<FeedEvent>
    suspend fun refresh()
    suspend fun syncOwnEvents()
}
