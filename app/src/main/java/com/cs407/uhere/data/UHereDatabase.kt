package com.cs407.uhere.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        User::class,
        Goal::class,
        LocationSession::class,
        Badge::class,
        UserBadge::class,
        Place::class  // NEW
    ],
    version = 2,  // INCREMENTED
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UHereDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun goalDao(): GoalDao
    abstract fun locationDao(): LocationDao
    abstract fun badgeDao(): BadgeDao
    abstract fun placeDao(): PlaceDao  // NEW

    companion object {
        @Volatile
        private var INSTANCE: UHereDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS places (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        category TEXT NOT NULL,
                        radius REAL NOT NULL,
                        userId INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getDatabase(context: Context): UHereDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UHereDatabase::class.java,
                    "uhere_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}