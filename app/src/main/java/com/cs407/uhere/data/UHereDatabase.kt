package com.cs407.uhere.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        User::class,
        Goal::class,
        LocationSession::class,
        Badge::class,
        UserBadge::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UHereDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun goalDao(): GoalDao
    abstract fun locationDao(): LocationDao
    abstract fun badgeDao(): BadgeDao

    companion object {
        @Volatile
        private var INSTANCE: UHereDatabase? = null

        fun getDatabase(context: Context): UHereDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UHereDatabase::class.java,
                    "uhere_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}