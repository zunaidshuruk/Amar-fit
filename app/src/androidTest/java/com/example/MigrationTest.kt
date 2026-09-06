package com.example

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate12To13() {
        var db = helper.createDatabase(TEST_DB, 12)
        // Insert some data in version 12
        db.execSQL("INSERT INTO user_profile (id, name, age, dateOfBirth, gender, heightCm, weightKg, dietaryRestrictions, healthGoals, dailyCalorieLimit, dailyWaterLimitLiters, currentStreak, points, badges, lastActiveDate, isDarkMode, notificationsEnabled, remindersEnabled, selectedLanguage) VALUES (1, 'Test User', 25, '2000-01-01', 'Male', 180.0, 75.0, 'None', 'Fit', 2000, 2.0, 0, 0, '', '2023-01-01', 0, 1, 1, 'English')")
        
        // Prepare for the next version.
        db.close()

        // Re-open the database with version 13 and provide MIGRATION_12_13 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 13, true, AppDatabase.MIGRATION_12_13)

        // Query to check if the new column exists and has the default value 0 (false)
        val cursor = db.query("SELECT onboardingCompleted, name FROM user_profile WHERE id = 1")
        assert(cursor.moveToFirst())
        
        val onboardingCompletedIndex = cursor.getColumnIndex("onboardingCompleted")
        val nameIndex = cursor.getColumnIndex("name")
        
        assert(cursor.getInt(onboardingCompletedIndex) == 0)
        assert(cursor.getString(nameIndex) == "Test User")
        
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate13To14() {
        var db = helper.createDatabase(TEST_DB, 13)
        // Insert some data in version 13
        db.execSQL("INSERT INTO saved_diet_charts (id, name, chartContent, shoppingList, createdAt) VALUES (1, 'Test Chart', 'Content', 'List', 123456789)")
        db.close()
        
        db = helper.runMigrationsAndValidate(TEST_DB, 14, true, AppDatabase.MIGRATION_13_14)
        
        val cursor = db.query("SELECT cloudId, name FROM saved_diet_charts WHERE id = 1")
        assert(cursor.moveToFirst())
        
        val cloudIdIndex = cursor.getColumnIndex("cloudId")
        val nameIndex = cursor.getColumnIndex("name")
        
        assert(cursor.getString(cloudIdIndex) == "")
        assert(cursor.getString(nameIndex) == "Test Chart")
        
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate14To15() {
        var db = helper.createDatabase(TEST_DB, 14)
        db.close()
        
        db = helper.runMigrationsAndValidate(TEST_DB, 15, true, AppDatabase.MIGRATION_14_15)
        
        // Check if the table exists by inserting into it
        db.execSQL("INSERT INTO saved_workouts (cloudId, title, content, createdAt) VALUES ('123-abc', 'Test Workout', 'Workout Content', 123456789)")
        val cursor = db.query("SELECT title FROM saved_workouts WHERE cloudId = '123-abc'")
        assert(cursor.moveToFirst())
        assert(cursor.getString(0) == "Test Workout")
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate15To16() {
        var db = helper.createDatabase(TEST_DB, 15)
        // Insert some data in version 15
        db.execSQL("INSERT INTO food_logs (id, date, name, category, calories, description, time, mealType) VALUES (1, '2026-09-05', 'Apple', 'Fruit', 95, 'Crisp apple', '12:00', 'Snack')")
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 16, true, AppDatabase.MIGRATION_15_16)

        val cursor = db.query("SELECT cloudId, name, calories FROM food_logs WHERE id = 1")
        assert(cursor.moveToFirst())

        val cloudIdIndex = cursor.getColumnIndex("cloudId")
        val nameIndex = cursor.getColumnIndex("name")
        val caloriesIndex = cursor.getColumnIndex("calories")

        assert(cursor.getString(cloudIdIndex) == "")
        assert(cursor.getString(nameIndex) == "Apple")
        assert(cursor.getInt(caloriesIndex) == 95)

        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate16To17() {
        var db = helper.createDatabase(TEST_DB, 16)
        // Insert some data in version 16
        db.execSQL("INSERT INTO daily_metrics (date, caloriesConsumed, waterLiters, steps, bloodGlucoseMorning, bloodGlucoseNight, bloodPressure, weightKg, sleepHours, heartRate) VALUES ('2026-09-05', 2100, 2.5, 8500, 95.0, 110.0, '120/80', 72.5, 7.5, 72)")
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 17, true, AppDatabase.MIGRATION_16_17)

        val cursor = db.query("SELECT distanceMeters, exerciseMinutes, steps, date FROM daily_metrics WHERE date = '2026-09-05'")
        assert(cursor.moveToFirst())

        val distanceIndex = cursor.getColumnIndex("distanceMeters")
        val exerciseIndex = cursor.getColumnIndex("exerciseMinutes")
        val stepsIndex = cursor.getColumnIndex("steps")
        val dateIndex = cursor.getColumnIndex("date")

        assert(cursor.getFloat(distanceIndex) == 0f)
        assert(cursor.getInt(exerciseIndex) == 0)
        assert(cursor.getInt(stepsIndex) == 8500)
        assert(cursor.getString(dateIndex) == "2026-09-05")

        cursor.close()
    }
}
