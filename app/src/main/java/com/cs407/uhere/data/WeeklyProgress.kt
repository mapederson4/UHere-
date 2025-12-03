package com.cs407.uhere.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a completed week's progress
 * Only created when at least one goal is completed in a week
 */
@Entity(tableName = "weekly_progress")
data class WeeklyProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // User who completed the goals
    val userId: Int,

    // Week boundaries (Sunday 12:00 AM to Saturday 11:59 PM)
    val weekStartDate: Long,
    val weekEndDate: Long,

    // Individual category completion status
    val libraryCompleted: Boolean = false,
    val barCompleted: Boolean = false,
    val gymCompleted: Boolean = false,

    // True if all three categories were completed
    val allGoalsCompleted: Boolean = false,

    // When this record was created
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Data class for displaying streak information
 * Used in UI to show user's achievement streaks
 */
data class StreakInfo(
    // null = "all goals" streak, otherwise specific category
    val category: LocationCategory?,

    // Consecutive weeks up to current week (or last completed week)
    val currentStreak: Int,

    // Longest consecutive streak ever achieved
    val bestStreak: Int,

    // Total number of weeks completed (not necessarily consecutive)
    val totalWeeksCompleted: Int
) {
    /**
     * Check if user has an active streak
     */
    fun hasActiveStreak(): Boolean = currentStreak > 0

    /**
     * Check if current streak matches or beats best streak
     */
    fun isPersonalBest(): Boolean = currentStreak == bestStreak && currentStreak > 0

    /**
     * Get a display-friendly description
     */
    fun getDescription(): String {
        return when {
            currentStreak == 0 -> "No active streak"
            currentStreak == 1 -> "1 week streak"
            else -> "$currentStreak weeks streak"
        }
    }
}