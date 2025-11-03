package com.cs407.uhere.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["firebaseUid"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val firebaseUid: String,
    val displayName: String,
    val email: String,
    val createdAt: Long = System.currentTimeMillis()
)