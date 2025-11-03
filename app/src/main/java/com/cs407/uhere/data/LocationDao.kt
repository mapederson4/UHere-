package com.cs407.uhere.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM location_sessions WHERE userId = :userId AND weekStartDate = :weekStart")
    fun getSessionsForWeek(userId: Int, weekStart: Long): Flow<List<LocationSession>>

    @Query("SELECT SUM(durationMinutes) FROM location_sessions WHERE userId = :userId AND locationCategory = :category AND weekStartDate = :weekStart")
    fun getTotalMinutesForCategory(userId: Int, category: LocationCategory, weekStart: Long): Flow<Int?>

    @Insert
    suspend fun insertSession(session: LocationSession): Long

    @Update
    suspend fun updateSession(session: LocationSession)
}