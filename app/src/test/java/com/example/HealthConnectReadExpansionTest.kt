package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.local.DailyMetric
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class HealthConnectReadExpansionTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testDefaultNewHealthConnectFields() {
        val metric = DailyMetric(date = "2026-09-06")
        assertEquals(0, metric.activeCaloriesBurned)
        assertEquals(0.0f, metric.heartRateVariability, 0.001f)
        assertEquals(0.0f, metric.oxygenSaturation, 0.001f)
        assertEquals(0.0f, metric.skinTemperatureCelsius, 0.001f)
        assertEquals(0.0f, metric.respiratoryRate, 0.001f)
        assertEquals(0, metric.mindfulnessMinutes)
    }

    @Test
    fun testRoomPersistsNewHealthConnectFields() = runBlocking {
        val metricsDao = db.metricsDao()
        val customMetric = DailyMetric(
            date = "2026-09-06",
            caloriesConsumed = 2000,
            activeCaloriesBurned = 450,
            heartRateVariability = 52.5f,
            oxygenSaturation = 98.2f,
            skinTemperatureCelsius = 33.4f,
            respiratoryRate = 14.5f,
            mindfulnessMinutes = 25
        )

        metricsDao.insertMetrics(customMetric)
        val loaded = metricsDao.getMetricsForDate("2026-09-06").first()

        assertNotNull(loaded)
        assertEquals(450, loaded?.activeCaloriesBurned)
        assertEquals(52.5f, loaded?.heartRateVariability ?: 0f, 0.001f)
        assertEquals(98.2f, loaded?.oxygenSaturation ?: 0f, 0.001f)
        assertEquals(33.4f, loaded?.skinTemperatureCelsius ?: 0f, 0.001f)
        assertEquals(14.5f, loaded?.respiratoryRate ?: 0f, 0.001f)
        assertEquals(25, loaded?.mindfulnessMinutes)
    }

    @Test
    fun testMigration22To23AddsAllFiveColumns() {
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext<Context>())
            .name("migration-test-22-23.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(22) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `daily_metrics` (`date` TEXT NOT NULL, `caloriesConsumed` INTEGER NOT NULL, `waterLiters` REAL NOT NULL, `steps` INTEGER NOT NULL, `bloodGlucoseMorning` REAL NOT NULL, `bloodGlucoseNight` REAL NOT NULL, `bloodPressure` TEXT NOT NULL, `weightKg` REAL NOT NULL, `sleepHours` REAL NOT NULL, `heartRate` INTEGER NOT NULL, `distanceMeters` REAL NOT NULL, `exerciseMinutes` INTEGER NOT NULL, `externalNutritionCalories` INTEGER NOT NULL, PRIMARY KEY(`date`))")
                    db.execSQL("INSERT INTO `daily_metrics` (`date`, `caloriesConsumed`, `waterLiters`, `steps`, `bloodGlucoseMorning`, `bloodGlucoseNight`, `bloodPressure`, `weightKg`, `sleepHours`, `heartRate`, `distanceMeters`, `exerciseMinutes`, `externalNutritionCalories`) VALUES ('2026-09-06', 2100, 2.0, 8000, 92.0, 105.0, '118/76', 68.0, 8.0, 65, 4500.0, 30, 250)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = helperFactory.create(config)
        val sqliteDb = openHelper.writableDatabase

        // Apply MIGRATION_22_23
        AppDatabase.MIGRATION_22_23.migrate(sqliteDb)

        val cursor = sqliteDb.query("SELECT activeCaloriesBurned, heartRateVariability, oxygenSaturation, skinTemperatureCelsius, respiratoryRate, caloriesConsumed FROM daily_metrics WHERE date = '2026-09-06'")
        assertEquals(true, cursor.moveToFirst())
        assertEquals(0, cursor.getInt(cursor.getColumnIndex("activeCaloriesBurned")))
        assertEquals(0.0f, cursor.getFloat(cursor.getColumnIndex("heartRateVariability")), 0.001f)
        assertEquals(0.0f, cursor.getFloat(cursor.getColumnIndex("oxygenSaturation")), 0.001f)
        assertEquals(0.0f, cursor.getFloat(cursor.getColumnIndex("skinTemperatureCelsius")), 0.001f)
        assertEquals(0.0f, cursor.getFloat(cursor.getColumnIndex("respiratoryRate")), 0.001f)
        assertEquals(2100, cursor.getInt(cursor.getColumnIndex("caloriesConsumed")))
        cursor.close()
        sqliteDb.close()
    }

    @Test
    fun testMigration23To24AddsMindfulnessMinutes() {
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext<Context>())
            .name("migration-test-23-24.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(23) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `daily_metrics` (`date` TEXT NOT NULL, `caloriesConsumed` INTEGER NOT NULL, `waterLiters` REAL NOT NULL, `steps` INTEGER NOT NULL, `bloodGlucoseMorning` REAL NOT NULL, `bloodGlucoseNight` REAL NOT NULL, `bloodPressure` TEXT NOT NULL, `weightKg` REAL NOT NULL, `sleepHours` REAL NOT NULL, `heartRate` INTEGER NOT NULL, `distanceMeters` REAL NOT NULL, `exerciseMinutes` INTEGER NOT NULL, `externalNutritionCalories` INTEGER NOT NULL, `activeCaloriesBurned` INTEGER NOT NULL, `heartRateVariability` REAL NOT NULL, `oxygenSaturation` REAL NOT NULL, `skinTemperatureCelsius` REAL NOT NULL, `respiratoryRate` REAL NOT NULL, PRIMARY KEY(`date`))")
                    db.execSQL("INSERT INTO `daily_metrics` (`date`, `caloriesConsumed`, `waterLiters`, `steps`, `bloodGlucoseMorning`, `bloodGlucoseNight`, `bloodPressure`, `weightKg`, `sleepHours`, `heartRate`, `distanceMeters`, `exerciseMinutes`, `externalNutritionCalories`, `activeCaloriesBurned`, `heartRateVariability`, `oxygenSaturation`, `skinTemperatureCelsius`, `respiratoryRate`) VALUES ('2026-09-06', 2100, 2.0, 8000, 92.0, 105.0, '118/76', 68.0, 8.0, 65, 4500.0, 30, 250, 450, 52.5, 98.2, 33.4, 14.5)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = helperFactory.create(config)
        val sqliteDb = openHelper.writableDatabase

        // Apply MIGRATION_23_24
        AppDatabase.MIGRATION_23_24.migrate(sqliteDb)

        val cursor = sqliteDb.query("SELECT mindfulnessMinutes, activeCaloriesBurned, caloriesConsumed FROM daily_metrics WHERE date = '2026-09-06'")
        assertEquals(true, cursor.moveToFirst())
        assertEquals(0, cursor.getInt(cursor.getColumnIndex("mindfulnessMinutes")))
        assertEquals(450, cursor.getInt(cursor.getColumnIndex("activeCaloriesBurned")))
        assertEquals(2100, cursor.getInt(cursor.getColumnIndex("caloriesConsumed")))
        cursor.close()
        sqliteDb.close()
    }
}
