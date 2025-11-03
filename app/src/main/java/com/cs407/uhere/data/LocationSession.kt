package com.cs407.uhere.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_sessions")
data class LocationSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val locationCategory: LocationCategory,
    val startTime: Long,
    val endTime: Long? = null,
    val durationMinutes: Int = 0,
    val weekStartDate: Long = getWeekStartDate()
)