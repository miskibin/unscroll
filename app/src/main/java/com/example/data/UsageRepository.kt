package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UsageRepository(private val usageEventDao: UsageEventDao) {

    fun recentEventsFlow(sinceMs: Long): Flow<List<LoggedEvent>> =
        usageEventDao.eventsSince(sinceMs).map { events ->
            events.map { LoggedEvent(it.timestampMs, it.outcome, it.reason) }
        }
}
