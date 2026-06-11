package com.example

import com.example.data.ScrollBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollBudgetTest {

    private val allowance = 120_000L // 2 min
    private val window = 600_000L // 10 min

    private fun budget() = ScrollBudget(allowance, window)

    @Test
    fun `fresh budget is not exhausted`() {
        val b = budget()
        assertFalse(b.isExhausted(1_000_000))
        assertEquals(allowance, b.remainingMs(1_000_000))
        assertEquals(0, b.msUntilReset(1_000_000))
    }

    @Test
    fun `continuous watching exhausts after allowance`() {
        val b = budget()
        var now = 1_000_000L
        var blocked = b.onSurfaceVisible(now)
        // 2 minutes of 1s ticks
        repeat(120) {
            now += 1_000
            blocked = b.onSurfaceVisible(now)
        }
        assertTrue(blocked)
        assertEquals(0, b.remainingMs(now))
    }

    @Test
    fun `long gaps are not charged`() {
        val b = budget()
        var now = 1_000_000L
        b.onSurfaceVisible(now)
        now += 60_000 // user left for a minute
        b.onSurfaceVisible(now)
        assertEquals(allowance, b.remainingMs(now))
    }

    @Test
    fun `allowance refills after the window`() {
        val b = budget()
        var now = 1_000_000L
        b.onSurfaceVisible(now)
        repeat(120) {
            now += 1_000
            b.onSurfaceVisible(now)
        }
        assertTrue(b.isExhausted(now))

        // window measured from first use
        now = 1_000_000L + window
        assertFalse(b.isExhausted(now))
        assertEquals(allowance, b.remainingMs(now))
        assertFalse(b.onSurfaceVisible(now))
    }

    @Test
    fun `reset countdown counts from first use`() {
        val b = budget()
        val start = 1_000_000L
        b.onSurfaceVisible(start)
        b.onSurfaceVisible(start + 1_000)
        assertEquals(window - 5_000, b.msUntilReset(start + 5_000))
    }

    @Test
    fun `restored state survives service restart`() {
        val b = budget()
        var now = 1_000_000L
        b.onSurfaceVisible(now)
        repeat(120) {
            now += 1_000
            b.onSurfaceVisible(now)
        }
        val restored = budget()
        restored.restore(b.windowStartAt, b.usedMs, now)
        assertTrue(restored.isExhausted(now))

        // and still refills on schedule
        assertFalse(restored.isExhausted(1_000_000L + window))
    }

    @Test
    fun `restored stale state is discarded`() {
        val restored = budget()
        restored.restore(windowStartAt = 0, usedMs = 120_000, now = window * 5)
        assertFalse(restored.isExhausted(window * 5))
    }
}
