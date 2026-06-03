package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageEventDao {
    @Insert
    suspend fun insert(event: UsageEvent)

    @Query("SELECT * FROM usage_events WHERE timestampMs >= :since ORDER BY timestampMs DESC")
    fun eventsSince(since: Long): Flow<List<UsageEvent>>
}
