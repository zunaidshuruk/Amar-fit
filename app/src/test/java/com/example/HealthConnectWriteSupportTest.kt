package com.example

import android.app.Application
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.health.HealthConnectManager
import com.example.data.local.AppDatabase
import com.example.data.local.DailyMetric
import com.example.presentation.viewmodel.ShasthoViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class HealthConnectWriteSupportTest {

    @Test
    fun testRequiredPermissionsContainAllFourWritePermissions() {
        val nutritionWrite = HealthPermission.getWritePermission(NutritionRecord::class)
        val hydrationWrite = HealthPermission.getWritePermission(HydrationRecord::class)
        val sleepWrite = HealthPermission.getWritePermission(SleepSessionRecord::class)
        val exerciseWrite = HealthPermission.getWritePermission(ExerciseSessionRecord::class)

        assertEquals("android.permission.health.WRITE_NUTRITION", nutritionWrite)
        assertEquals("android.permission.health.WRITE_HYDRATION", hydrationWrite)
        assertEquals("android.permission.health.WRITE_SLEEP", sleepWrite)
        assertEquals("android.permission.health.WRITE_EXERCISE", exerciseWrite)

        assertTrue(HealthConnectManager.REQUIRED_PERMISSIONS.contains(nutritionWrite))
        assertTrue(HealthConnectManager.REQUIRED_PERMISSIONS.contains(hydrationWrite))
        assertTrue(HealthConnectManager.REQUIRED_PERMISSIONS.contains(sleepWrite))
        assertTrue(HealthConnectManager.REQUIRED_PERMISSIONS.contains(exerciseWrite))
    }

    @Test
    fun testManifestAndXmlContainMatchingWritePermissions() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        val xmlFile = File("src/main/res/values/health_permissions.xml")

        val expectedPermissions = listOf(
            "android.permission.health.WRITE_NUTRITION",
            "android.permission.health.WRITE_HYDRATION",
            "android.permission.health.WRITE_SLEEP",
            "android.permission.health.WRITE_EXERCISE"
        )

        val manifestContent = if (manifestFile.exists()) manifestFile.readText() else File("app/src/main/AndroidManifest.xml").readText()
        val xmlContent = if (xmlFile.exists()) xmlFile.readText() else File("app/src/main/res/values/health_permissions.xml").readText()

        for (perm in expectedPermissions) {
            assertTrue("Manifest should contain $perm", manifestContent.contains(perm))
            assertTrue("health_permissions.xml should contain $perm", xmlContent.contains("<item>$perm</item>"))
        }
    }

    @Test
    fun testSaveCompletedWorkoutSessionIncrementsMetrics() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ShasthoViewModel(app)
        val db = AppDatabase.getDatabase(app)
        val today = viewModel.todayDateString

        // Seed an initial daily metric
        db.metricsDao().insertMetrics(
            DailyMetric(
                date = today,
                exerciseMinutes = 15,
                activeCaloriesBurned = 100
            )
        )

        val start = Instant.now().minusSeconds(1200)
        val end = Instant.now()

        // 20 minutes (1200 seconds), 150 calories
        viewModel.saveCompletedWorkoutSession(
            planTitle = "Full Body HIIT",
            startTime = start,
            endTime = end,
            totalElapsedSeconds = 1200,
            caloriesBurned = 150.0
        )

        val updatedMetric = db.metricsDao().getMetricsForDate(today).first()
        assertNotNull(updatedMetric)
        assertEquals(35, updatedMetric?.exerciseMinutes) // 15 + 20 = 35
        assertEquals(250, updatedMetric?.activeCaloriesBurned) // 100 + 150 = 250
    }

    @Test
    fun testSaveCompletedWorkoutSessionNullCaloriesSkipsCalorieIncrement() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ShasthoViewModel(app)
        val db = AppDatabase.getDatabase(app)
        val today = viewModel.todayDateString

        db.metricsDao().insertMetrics(
            DailyMetric(
                date = today,
                exerciseMinutes = 10,
                activeCaloriesBurned = 80
            )
        )

        val start = Instant.now().minusSeconds(600)
        val end = Instant.now()

        // 10 minutes (600 seconds), null calories
        viewModel.saveCompletedWorkoutSession(
            planTitle = "Core Burn",
            startTime = start,
            endTime = end,
            totalElapsedSeconds = 600,
            caloriesBurned = null
        )

        val updatedMetric = db.metricsDao().getMetricsForDate(today).first()
        assertNotNull(updatedMetric)
        assertEquals(20, updatedMetric?.exerciseMinutes) // 10 + 10 = 20
        assertEquals(80, updatedMetric?.activeCaloriesBurned) // unchanged at 80
    }

    @Test
    fun testSaveCompletedWorkoutSessionDeduplication() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ShasthoViewModel(app)
        val db = AppDatabase.getDatabase(app)
        val today = viewModel.todayDateString

        db.metricsDao().insertMetrics(
            DailyMetric(
                date = today,
                exerciseMinutes = 0,
                activeCaloriesBurned = 0
            )
        )

        val start = Instant.now().minusSeconds(300)
        val end = Instant.now()

        // First call
        viewModel.saveCompletedWorkoutSession(
            planTitle = "Duplicate Test",
            startTime = start,
            endTime = end,
            totalElapsedSeconds = 300,
            caloriesBurned = 50.0
        )

        // Duplicate call with exact same plan title and start time
        viewModel.saveCompletedWorkoutSession(
            planTitle = "Duplicate Test",
            startTime = start,
            endTime = end,
            totalElapsedSeconds = 300,
            caloriesBurned = 50.0
        )

        val updatedMetric = db.metricsDao().getMetricsForDate(today).first()
        assertNotNull(updatedMetric)
        assertEquals(5, updatedMetric?.exerciseMinutes) // 5 minutes, not 10
        assertEquals(50, updatedMetric?.activeCaloriesBurned) // 50 calories, not 100
    }
}
