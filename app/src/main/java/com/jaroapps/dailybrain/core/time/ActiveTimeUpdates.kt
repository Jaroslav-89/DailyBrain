package com.jaroapps.dailybrain.core.time

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/**
 * Emits elapsed active time at a fixed interval while the collecting coroutine is active.
 * The tracker itself remains responsible for excluding paused intervals.
 */
fun ActiveTimeTracker.elapsedTimeUpdates(
    intervalMs: Long = 100L,
): Flow<Long> = flow {
    require(intervalMs > 0L) { "intervalMs must be positive" }

    while (currentCoroutineContext().isActive) {
        emit(elapsedMs())
        delay(intervalMs)
    }
}
