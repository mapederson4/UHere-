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
        Place::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UHereDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun goalDao(): GoalDao
    abstract fun locationDao(): LocationDao
    abstract fun badgeDao(): BadgeDao
    abstract fun placeDao(): PlaceDao

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
                // Add weekStartDate column if it doesn't exist
                try {
                    database.execSQL("ALTER TABLE goals ADD COLUMN weekStartDate INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // Column might already exist, ignore
                }

                // Add isActive column if it doesn't exist
                try {
                    database.execSQL("ALTER TABLE goals ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                } catch (e: Exception) {
                    // Column might already exist, ignore
                }
            }
        }

        fun getDatabase(context: Context): UHereDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UHereDatabase::class.java,
                    "uhere_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}