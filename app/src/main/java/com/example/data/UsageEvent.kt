package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One recorded moment at a blocked app (a pause resolution). */
@Entity(tableName = "usage_events")
data class UsageEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val outcome: String,
    val reason: String? = null,
)
