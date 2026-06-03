package com.example.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fire-and-forget recorder for pause outcomes. Uses an application-scoped coroutine (not the
 * Activity's) so the write survives the screen finishing immediately after the user's choice.
 */
object EventLogger {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun log(context: Context, outcome: String, reason: String?) {
        val dao = AppDatabase.getDatabase(context.applicationContext).usageEventDao()
        scope.launch {
            dao.insert(
                UsageEvent(
                    timestampMs = System.currentTimeMillis(),
                    outcome = outcome,
                    reason = reason,
                )
            )
        }
    }
}
