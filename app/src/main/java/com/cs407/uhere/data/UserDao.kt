package com.cs407.uhere.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE firebaseUid = :uid")
    suspend fun getUserByFirebaseUid(uid: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM users WHERE firebaseUid = :uid")
    fun getUserByFirebaseUidFlow(uid: String): Flow<User?>
}