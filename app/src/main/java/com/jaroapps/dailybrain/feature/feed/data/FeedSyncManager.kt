package com.jaroapps.dailybrain.feature.feed.data

import com.jaroapps.dailybrain.feature.feed.domain.FeedRepository
import com.jaroapps.dailybrain.feature.feed.domain.FeedSyncTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FeedSyncManager(
    private val repository: FeedRepository,
) : FeedSyncTrigger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    override fun requestSync() {
        scope.launch {
            mutex.withLock {
                runCatching { repository.syncOwnEvents() }
            }
        }
    }
}
