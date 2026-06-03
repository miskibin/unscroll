package com.example

import com.example.data.EventOutcome
import com.example.data.Insights
import com.example.data.LoggedEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsTest {

    private val now = 1_000_000_000_000L
    private val hour = 60 * 60 * 1000L

    private fun events() = listOf(
        LoggedEvent(now - 1 * hour, EventOutcome.TURNED_AWAY, "bored"),
        LoggedEvent(now - 2 * hour, EventOutcome.TURNED_AWAY, "browsing"),
        LoggedEvent(now - 3 * hour, EventOutcome.ENTERED, "message"),
        LoggedEvent(now - 4 * hour, EventOutcome.BONUS, null),
        LoggedEvent(now - 100 * hour, EventOutcome.ENTERED, "bored"), // outside a 24h window
    )

    @Test
    fun `counts outcomes within the window and excludes older events`() {
        val summary = Insights.summarize(events(), sinceMs = now - 24 * hour)
        assertEquals(1, summary.entered)
        assertEquals(2, summary.turnedAway)
        assertEquals(1, summary.bonus)
        assertEquals(4, summary.attempts)
    }

    @Test
    fun `turn-away rate reflects choices not to scroll`() {
        val summary = Insights.summarize(events(), sinceMs = now - 24 * hour)
        // 2 turn-aways out of 4 attempts.
        assertEquals(0.5f, summary.turnAwayRate, 0.0001f)
    }

    @Test
    fun `reasons are grouped, nulls ignored`() {
        val summary = Insights.summarize(events(), sinceMs = now - 24 * hour)
        assertEquals(1, summary.byReason["bored"])
        assertEquals(1, summary.byReason["browsing"])
        assertEquals(1, summary.byReason["message"])
        assertEquals(null, summary.byReason["nope"])
    }

    @Test
    fun `empty history is safe`() {
        val summary = Insights.summarize(emptyList(), sinceMs = 0)
        assertEquals(0, summary.attempts)
        assertEquals(0f, summary.turnAwayRate, 0.0001f)
    }

    @Test
    fun `start of today is midnight at or before now and within 24h`() {
        val start = Insights.startOfToday(now)
        assertTrue(start <= now)
        assertTrue(now - start < 24 * hour)
    }
}
