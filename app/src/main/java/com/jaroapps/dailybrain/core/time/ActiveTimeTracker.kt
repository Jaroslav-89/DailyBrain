package com.jaroapps.dailybrain.core.time

import android.os.SystemClock

/**
 * Tracks elapsed time using a monotonic clock and excludes intervals while paused.
 *
 * Calls to [resume] and [pause] are idempotent, so lifecycle callbacks can safely
 * arrive more than once.
 */
class ActiveTimeTracker(
    private val nowMs: () -> Long = SystemClock::elapsedRealtime,
) {
    private var accumulatedMs = 0L
    private var activeSegmentStartedAtMs: Long? = null

    val isRunning: Boolean
        get() = activeSegmentStartedAtMs != null

    fun resume() {
        if (activeSegmentStartedAtMs == null) {
            activeSegmentStartedAtMs = nowMs()
        }
    }

    fun pause() {
        val segmentStartedAtMs = activeSegmentStartedAtMs ?: return
        accumulatedMs += (nowMs() - segmentStartedAtMs).coerceAtLeast(0L)
        activeSegmentStartedAtMs = null
    }

    fun elapsedMs(): Long {
        val activeSegmentMs = activeSegmentStartedAtMs?.let { segmentStartedAtMs ->
            (nowMs() - segmentStartedAtMs).coerceAtLeast(0L)
        } ?: 0L
        return accumulatedMs + activeSegmentMs
    }

    fun reset() {
        accumulatedMs = 0L
        activeSegmentStartedAtMs = null
    }
}
