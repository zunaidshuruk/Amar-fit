package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UserProfile::class, DailyMetric::class, FoodLog::class, SavedDietChart::class, SavedWorkout::class, SavedChat::class, ActivityEvent::class], version = 21, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun metricsDao(): MetricsDao
    abstract fun savedDietChartDao(): SavedDietChartDao
    abstract fun savedWorkoutDao(): SavedWorkoutDao
    abstract fun savedChatDao(): SavedChatDao
    abstract fun activityEventDao(): ActivityEventDao

    companion object {
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profile ADD COLUMN dateOfBirth TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profile ADD COLUMN onboardingCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE saved_diet_charts ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `saved_workouts` (`cloudId` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`cloudId`))")
            }
        }
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE food_logs ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_metrics ADD COLUMN distanceMeters REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE daily_metrics ADD COLUMN exerciseMinutes INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE saved_diet_charts SET cloudId = hex(randomblob(16)) WHERE cloudId = '' OR cloudId IS NULL")
            }
        }
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_metrics ADD COLUMN externalNutritionCalories INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `saved_chats` (`cloudId` TEXT NOT NULL, `title` TEXT NOT NULL, `messages` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`cloudId`))")
            }
        }
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `activity_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `description` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shastho_database"
                )
                .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
