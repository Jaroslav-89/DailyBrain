package com.jaroapps.dailybrain.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveTimeTrackerTest {

    @Test
    fun elapsedTimeExcludesPausedIntervals() {
        var nowMs = 1_000L
        val tracker = ActiveTimeTracker { nowMs }

        tracker.resume()
        nowMs += 500L
        assertEquals(500L, tracker.elapsedMs())

        tracker.pause()
        nowMs += 2_000L
        assertEquals(500L, tracker.elapsedMs())

        tracker.resume()
        nowMs += 300L
        tracker.pause()

        assertEquals(800L, tracker.elapsedMs())
        assertFalse(tracker.isRunning)
    }

    @Test
    fun repeatedResumeAndPauseAreIdempotent() {
        var nowMs = 0L
        val tracker = ActiveTimeTracker { nowMs }

        tracker.resume()
        nowMs += 100L
        tracker.resume()
        nowMs += 100L
        tracker.pause()
        tracker.pause()

        assertEquals(200L, tracker.elapsedMs())
        assertFalse(tracker.isRunning)
    }

    @Test
    fun resetClearsElapsedTimeAndRunningState() {
        var nowMs = 0L
        val tracker = ActiveTimeTracker { nowMs }

        tracker.resume()
        nowMs += 250L
        tracker.reset()

        assertEquals(0L, tracker.elapsedMs())
        assertFalse(tracker.isRunning)

        tracker.resume()
        nowMs += 75L
        assertTrue(tracker.isRunning)
        assertEquals(75L, tracker.elapsedMs())
    }
}
