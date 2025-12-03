package com.cs407.uhere.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for WeeklyProgress entity
 * Handles all database operations for weekly progress tracking
 */
@Dao
interface WeeklyProgressDao {

    /**
     * Get all weekly progress records for a user, ordered by most recent first
     */
    @Query("SELECT * FROM weekly_progress WHERE userId = :userId ORDER BY weekStartDate DESC")
    fun getAllWeeklyProgress(userId: Int): Flow<List<WeeklyProgress>>

    /**
     * Get weekly progress for a specific week
     * Returns null if no progress recorded for that week
     */
    @Query("SELECT * FROM weekly_progress WHERE userId = :userId AND weekStartDate = :weekStart")
    suspend fun getWeeklyProgress(userId: Int, weekStart: Long): WeeklyProgress?

    /**
     * Insert or replace a weekly progress record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyProgress(weeklyProgress: WeeklyProgress): Long

    /**
     * Update an existing weekly progress record
     */
    @Update
    suspend fun updateWeeklyProgress(weeklyProgress: WeeklyProgress)

    /**
     * Delete a specific weekly progress record
     */
    @Delete
    suspend fun deleteWeeklyProgress(weeklyProgress: WeeklyProgress)

    /**
     * Get all weeks where a specific category goal was completed
     * Ordered by most recent first
     */
    @Query("""
        SELECT * FROM weekly_progress 
        WHERE userId = :userId 
        AND (
            (:category = 'LIBRARY' AND libraryCompleted = 1) OR
            (:category = 'BAR' AND barCompleted = 1) OR
            (:category = 'GYM' AND gymCompleted = 1)
        )
        ORDER BY weekStartDate DESC
    """)
    suspend fun getCompletedWeeksForCategory(userId: Int, category: String): List<WeeklyProgress>

    /**
     * Get all weeks where ALL goals were completed
     * Ordered by most recent first
     */
    @Query("""
        SELECT * FROM weekly_progress 
        WHERE userId = :userId AND allGoalsCompleted = 1
        ORDER BY weekStartDate DESC
    """)
    suspend fun getAllGoalsCompletedWeeks(userId: Int): List<WeeklyProgress>

    /**
     * Count total weeks completed for a specific category
     */
    @Query("""
        SELECT COUNT(*) FROM weekly_progress 
        WHERE userId = :userId 
        AND (
            (:category = 'LIBRARY' AND libraryCompleted = 1) OR
            (:category = 'BAR' AND barCompleted = 1) OR
            (:category = 'GYM' AND gymCompleted = 1)
        )
    """)
    suspend fun getTotalWeeksCompletedForCategory(userId: Int, category: String): Int

    /**
     * Count total weeks where all goals were completed
     */
    @Query("SELECT COUNT(*) FROM weekly_progress WHERE userId = :userId AND allGoalsCompleted = 1")
    suspend fun getTotalAllGoalsCompletedWeeks(userId: Int): Int

    /**
     * Get the most recent week's progress
     */
    @Query("""
        SELECT * FROM weekly_progress 
        WHERE userId = :userId 
        ORDER BY weekStartDate DESC 
        LIMIT 1
    """)
    suspend fun getMostRecentWeeklyProgress(userId: Int): WeeklyProgress?

    /**
     * Check if a specific week has been recorded
     */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM weekly_progress 
            WHERE userId = :userId AND weekStartDate = :weekStart
        )
    """)
    suspend fun hasWeekBeenRecorded(userId: Int, weekStart: Long): Boolean

    /**
     * Delete all weekly progress for a user (for account deletion or reset)
     */
    @Query("DELETE FROM weekly_progress WHERE userId = :userId")
    suspend fun deleteAllUserProgress(userId: Int)

    /**
     * Get weeks in a date range
     */
    @Query("""
        SELECT * FROM weekly_progress 
        WHERE userId = :userId 
        AND weekStartDate >= :startDate 
        AND weekStartDate <= :endDate
        ORDER BY weekStartDate DESC
    """)
    suspend fun getProgressInRange(
        userId: Int,
        startDate: Long,
        endDate: Long
    ): List<WeeklyProgress>
}