package com.cs407.uhere.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for GoalCompletion entity
 * Handles instant goal completion tracking for badges
 *
 * CREATE THIS FILE: In your project at:
 * app/src/main/java/com/cs407/uhere/data/GoalCompletionDao.kt
 */
@Dao
interface GoalCompletionDao {

    /**
     * Get all completions for a user, ordered by most recent first
     */
    @Query("SELECT * FROM goal_completions WHERE userId = :userId ORDER BY completedAt DESC")
    fun getAllCompletions(userId: Int): Flow<List<GoalCompletion>>

    /**
     * Get all completions for a specific category
     */
    @Query("""
        SELECT * FROM goal_completions 
        WHERE userId = :userId AND category = :category 
        ORDER BY completedAt DESC
    """)
    fun getCompletionsForCategory(userId: Int, category: LocationCategory): Flow<List<GoalCompletion>>

    /**
     * Count total completions for a category (for badge unlocking)
     */
    @Query("""
        SELECT COUNT(*) FROM goal_completions 
        WHERE userId = :userId AND category = :category
    """)
    suspend fun getTotalCompletionsForCategory(userId: Int, category: LocationCategory): Int

    /**
     * Get completion for a specific week and category
     * Returns null if not yet completed this week
     */
    @Query("""
        SELECT * FROM goal_completions 
        WHERE userId = :userId AND weekStartDate = :weekStart AND category = :category
    """)
    suspend fun getCompletionForWeek(userId: Int, weekStart: Long, category: LocationCategory): GoalCompletion?

    /**
     * Insert a new completion (IGNORE on conflict to prevent duplicates)
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: GoalCompletion): Long

    /**
     * Delete all completions for a user (for account deletion)
     */
    @Query("DELETE FROM goal_completions WHERE userId = :userId")
    suspend fun deleteAllUserCompletions(userId: Int)

    /**
     * Get completions in date range
     */
    @Query("""
        SELECT * FROM goal_completions 
        WHERE userId = :userId 
        AND weekStartDate >= :startDate 
        AND weekStartDate <= :endDate
        ORDER BY completedAt DESC
    """)
    suspend fun getCompletionsInRange(
        userId: Int,
        startDate: Long,
        endDate: Long
    ): List<GoalCompletion>

    /**
     * Get completions grouped by week for streak calculation
     */
    @Query("""
        SELECT * FROM goal_completions 
        WHERE userId = :userId AND category = :category 
        ORDER BY weekStartDate ASC
    """)
    suspend fun getCompletionsForStreakCalculation(userId: Int, category: LocationCategory): List<GoalCompletion>
}