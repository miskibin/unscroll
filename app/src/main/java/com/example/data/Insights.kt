package com.example.data

import java.util.Calendar

/** A single recorded moment at a blocked app, decoupled from Room for easy testing. */
data class LoggedEvent(
    val timestampMs: Long,
    val outcome: String,
    val reason: String?,
)

/** The outcomes we record. Kept as plain strings so they're stable in the database. */
object EventOutcome {
    const val ENTERED = "entered"        // went in after the conscious pause
    const val TURNED_AWAY = "turned_away" // chose not to — the win we celebrate
    const val BONUS = "bonus"            // earned their way past a focus block via the quiz
}

/**
 * Pure aggregation of recorded moments into the numbers the dashboard reflects back.
 * Self-monitoring + feedback is among the most effective behaviour-change techniques, so the
 * framing here is deliberately positive (turn-aways are "moments for yourself"), never shaming.
 */
object Insights {

    data class Summary(
        val entered: Int,
        val turnedAway: Int,
        val bonus: Int,
        val byReason: Map<String, Int>,
    ) {
        /** Every time the pause resolved one way or another. */
        val attempts: Int get() = entered + turnedAway + bonus

        /** Fraction of attempts where the user chose not to scroll (0f when no data). */
        val turnAwayRate: Float get() = if (attempts == 0) 0f else turnedAway.toFloat() / attempts
    }

    fun summarize(events: List<LoggedEvent>, sinceMs: Long): Summary {
        val inWindow = events.filter { it.timestampMs >= sinceMs }
        return Summary(
            entered = inWindow.count { it.outcome == EventOutcome.ENTERED },
            turnedAway = inWindow.count { it.outcome == EventOutcome.TURNED_AWAY },
            bonus = inWindow.count { it.outcome == EventOutcome.BONUS },
            byReason = inWindow.mapNotNull { it.reason }.groupingBy { it }.eachCount(),
        )
    }

    /** Local midnight for the day containing [now] — used for the "today" window. */
    fun startOfToday(now: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
