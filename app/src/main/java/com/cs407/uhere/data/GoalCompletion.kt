package com.cs407.uhere.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks goal completions as they happen (when progress reaches 100%)
 * This enables instant badge unlocking without waiting for week to end
 *
 */
@Entity(
    tableName = "goal_completions",
    indices = [Index(value = ["userId", "weekStartDate", "category"], unique = true)]
)
data class GoalCompletion(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: Int,

    val category: LocationCategory,

    val weekStartDate: Long,

    val completedAt: Long, // Timestamp when 100% was first reached

    val targetHours: Float,

    val completedMinutes: Int
)