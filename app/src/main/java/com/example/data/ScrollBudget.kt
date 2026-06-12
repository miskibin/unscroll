package com.example.data

/**
 * Rolling scroll allowance: the user may watch short-form feeds for
 * [allowanceMs] within a window of [windowMs]. The window starts when the
 * first second of allowance is consumed; once it expires the allowance
 * refills in full.
 *
 * Pure logic, no Android dependencies — driven by timestamps passed in.
 */
class ScrollBudget(
    var allowanceMs: Long,
    var windowMs: Long,
) {
    var windowStartAt: Long = 0L
        private set
    var usedMs: Long = 0L
        private set
    private var lastSeenAt: Long = 0L

    /** Gaps between sightings longer than this are not counted as watch time. */
    private val maxCountedGapMs = 2_500L

    /**
     * Record that a short-form surface is on screen at [now].
     * Returns true if the allowance is exhausted and the surface must be blocked.
     */
    fun onSurfaceVisible(now: Long): Boolean {
        rollWindow(now)
        val gap = now - lastSeenAt
        val startOfStreak = lastSeenAt == 0L || gap > maxCountedGapMs
        // Anchor the window at the first sighting of a charging streak, so the
        // tick that charges 0→1s doesn't move the window start.
        if (usedMs == 0L && startOfStreak) {
            windowStartAt = now
        }
        if (!startOfStreak && gap > 0) {
            usedMs += gap
        }
        lastSeenAt = now
        return isExhausted(now)
    }

    fun isExhausted(now: Long): Boolean {
        rollWindow(now)
        return usedMs >= allowanceMs
    }

    fun remainingMs(now: Long): Long {
        rollWindow(now)
        return (allowanceMs - usedMs).coerceAtLeast(0)
    }

    /** Millis until the allowance refills; 0 when nothing has been used yet. */
    fun msUntilReset(now: Long): Long {
        rollWindow(now)
        if (usedMs == 0L) return 0
        return (windowStartAt + windowMs - now).coerceAtLeast(0)
    }

    fun restore(windowStartAt: Long, usedMs: Long, now: Long) {
        this.windowStartAt = windowStartAt
        this.usedMs = usedMs
        this.lastSeenAt = 0
        rollWindow(now)
    }

    private fun rollWindow(now: Long) {
        if (usedMs > 0 && now - windowStartAt >= windowMs) {
            usedMs = 0
            windowStartAt = 0
            lastSeenAt = 0
        }
    }
}
