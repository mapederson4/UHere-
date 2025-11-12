package com.cs407.uhere.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places WHERE userId = :userId")
    fun getUserPlaces(userId: Int): Flow<List<Place>>

    @Query("SELECT * FROM places WHERE userId = :userId AND category = :category")
    fun getPlacesByCategory(userId: Int, category: LocationCategory): Flow<List<Place>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: Place): Long

    @Update
    suspend fun updatePlace(place: Place)

    @Delete
    suspend fun deletePlace(place: Place)

    @Query("DELETE FROM places WHERE userId = :userId")
    suspend fun deleteAllUserPlaces(userId: Int)
}