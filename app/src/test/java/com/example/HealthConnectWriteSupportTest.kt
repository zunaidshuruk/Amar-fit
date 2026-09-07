package com.example

import android.content.Context
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.health.HealthConnectManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class HealthConnectWriteSupportTest {

    @Test
    fun testRequiredPermissionsContainAllThreeWritePermissions() {
        val nutritionWrite = HealthPermission.getWritePermission(NutritionRecord::class)
        val hydrationWrite = HealthPermission.getWritePermission(HydrationRecord::class)
        val sleepWrite = HealthPermission.getWritePermission(SleepSessionRecord::class)

        assertEquals("android.permission.health.WRITE_NUTRITION", nutritionWrite)
        assertEquals("android.permission.health.WRITE_HYDRATION", hydrationWrite)
        assertEquals("android.permission.health.WRITE_SLEEP", sleepWrite)

        assertTrue(HealthConnectManager.REQUIRED_PERMISSIONS.contains(nutritionWrite))
        assertTrue(HealthConnectManager.REQUIRED_PERMISSIONS.contains(hydrationWrite))
        assertTrue(HealthConnectManager.REQUIRED_PERMISSIONS.contains(sleepWrite))
    }

    @Test
    fun testManifestAndXmlContainMatchingWritePermissions() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        val xmlFile = File("src/main/res/values/health_permissions.xml")

        val expectedPermissions = listOf(
            "android.permission.health.WRITE_NUTRITION",
            "android.permission.health.WRITE_HYDRATION",
            "android.permission.health.WRITE_SLEEP"
        )

        val manifestContent = if (manifestFile.exists()) manifestFile.readText() else File("app/src/main/AndroidManifest.xml").readText()
        val xmlContent = if (xmlFile.exists()) xmlFile.readText() else File("app/src/main/res/values/health_permissions.xml").readText()

        for (perm in expectedPermissions) {
            assertTrue("Manifest should contain $perm", manifestContent.contains(perm))
            assertTrue("health_permissions.xml should contain $perm", xmlContent.contains("<item>$perm</item>"))
        }
    }
}
