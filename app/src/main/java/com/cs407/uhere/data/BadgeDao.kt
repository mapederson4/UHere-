package com.cs407.uhere.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {
    @Query("SELECT * FROM badges")
    fun getAllBadges(): Flow<List<Badge>>

    @Query("SELECT b.* FROM badges b INNER JOIN user_badges ub ON b.id = ub.badgeId WHERE ub.userId = :userId")
    fun getUserBadges(userId: Int): Flow<List<Badge>>

    @Insert
    suspend fun insertUserBadge(userBadge: UserBadge)

    @Query("SELECT EXISTS(SELECT 1 FROM user_badges WHERE userId = :userId AND badgeId = :badgeId)")
    suspend fun hasUserEarnedBadge(userId: Int, badgeId: Int): Boolean

    @Insert
    suspend fun insertBadge(badge: Badge)
}