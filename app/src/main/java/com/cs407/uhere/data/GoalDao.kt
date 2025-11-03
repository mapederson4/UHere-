package com.cs407.uhere.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE userId = :userId AND isActive = 1")
    fun getActiveGoals(userId: Int): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE userId = :userId AND locationCategory = :category AND weekStartDate = :weekStart AND isActive = 1")
    suspend fun getGoalForWeek(userId: Int, category: LocationCategory, weekStart: Long): Goal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Query("UPDATE goals SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivateAllGoals(userId: Int)
}