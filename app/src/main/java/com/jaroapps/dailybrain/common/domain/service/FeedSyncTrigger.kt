package com.jaroapps.dailybrain.common.domain.service

/** Non-blocking signal used after a training is saved or removed. */
interface FeedSyncTrigger {
    fun requestSync()
}
