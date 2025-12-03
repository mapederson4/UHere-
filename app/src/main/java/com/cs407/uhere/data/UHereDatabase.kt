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
        Place::class,
        WeeklyProgress::class  // ADDED
    ],
    version = 4,  // UPDATED from 3 to 4
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UHereDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun goalDao(): GoalDao
    abstract fun locationDao(): LocationDao
    abstract fun badgeDao(): BadgeDao
    abstract fun placeDao(): PlaceDao
    abstract fun weeklyProgressDao(): WeeklyProgressDao  // ADDED

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE goals ADD COLUMN weekStartDate INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // Column might already exist, ignore
                }

                try {
                    database.execSQL("ALTER TABLE goals ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                } catch (e: Exception) {
                    // Column might already exist, ignore
                }
            }
        }

        // ADDED: Migration for WeeklyProgress table
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS weekly_progress (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        weekStartDate INTEGER NOT NULL,
                        weekEndDate INTEGER NOT NULL,
                        libraryCompleted INTEGER NOT NULL DEFAULT 0,
                        barCompleted INTEGER NOT NULL DEFAULT 0,
                        gymCompleted INTEGER NOT NULL DEFAULT 0,
                        allGoalsCompleted INTEGER NOT NULL DEFAULT 0,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)  // ADDED MIGRATION_3_4
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}