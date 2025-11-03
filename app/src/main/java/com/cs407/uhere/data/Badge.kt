package com.cs407.uhere.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val badgeName: String,
    val description: String,
    val iconResource: Int,
    val category: LocationCategory,
    val requirement: String
)

@Entity(tableName = "user_badges")
data class UserBadge(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val badgeId: Int,
    val earnedAt: Long = System.currentTimeMillis()
)