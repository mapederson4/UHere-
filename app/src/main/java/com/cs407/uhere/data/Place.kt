package com.cs407.uhere.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class Place(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: LocationCategory,
    val radius: Double = 100.0, // meters
    val userId: Int,
    val createdAt: Long = System.currentTimeMillis()
)