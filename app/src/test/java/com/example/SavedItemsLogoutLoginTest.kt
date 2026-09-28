package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.local.SavedChat
import com.example.data.local.SavedDietChart
import com.example.data.local.SavedWorkout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Constructor

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SavedItemsLogoutLoginTest {

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
    fun testZeroArgConstructorAvailability() {
        // Firestore's toObject() requires a zero-argument constructor to instantiate objects via reflection.
        val workoutConstructor: Constructor<SavedWorkout> = SavedWorkout::class.java.getDeclaredConstructor()
        assertNotNull(workoutConstructor)
        val defaultWorkout = workoutConstructor.newInstance()
        assertEquals("", defaultWorkout.title)
        assertEquals("", defaultWorkout.content)
        assertTrue(defaultWorkout.cloudId.isNotEmpty())

        val dietChartConstructor: Constructor<SavedDietChart> = SavedDietChart::class.java.getDeclaredConstructor()
        assertNotNull(dietChartConstructor)
        val defaultDietChart = dietChartConstructor.newInstance()
        assertEquals("", defaultDietChart.name)
        assertEquals("", defaultDietChart.chartContent)
        assertEquals("", defaultDietChart.shoppingList)
        assertTrue(defaultDietChart.cloudId.isNotEmpty())

        val chatConstructor: Constructor<SavedChat> = SavedChat::class.java.getDeclaredConstructor()
        assertNotNull(chatConstructor)
        val defaultChat = chatConstructor.newInstance()
        assertEquals("", defaultChat.title)
        assertEquals("", defaultChat.messages)
        assertTrue(defaultChat.cloudId.isNotEmpty())
    }

    @Test
    fun testSavedWorkoutLogoutAndRestoreCycle() = runBlocking {
        val workoutDao = db.savedWorkoutDao()

        // 1. User saves a workout locally (and pushes to Firestore)
        val originalWorkout = SavedWorkout(
            cloudId = "workout-uuid-123",
            title = "Morning Calisthenics",
            content = "3x15 Pushups\n3x20 Squats\n3x10 Pullups",
            createdAt = 1700000000000L
        )
        workoutDao.insertWorkout(originalWorkout)

        // Verify it exists in Room before logout
        val beforeLogout = workoutDao.getAllSavedWorkouts().first()
        assertEquals(1, beforeLogout.size)
        assertEquals("Morning Calisthenics", beforeLogout[0].title)
        assertEquals("workout-uuid-123", beforeLogout[0].cloudId)

        // 2. User logs out -> Room tables are wiped
        db.clearAllTables()
        val afterLogout = workoutDao.getAllSavedWorkouts().first()
        assertTrue(afterLogout.isEmpty())

        // 3. User logs back in -> Firestore pullDataOnLogin deserializes using zero-arg constructor
        val constructor = SavedWorkout::class.java.getDeclaredConstructor()
        val restoredWorkoutInstance = constructor.newInstance().copy(
            cloudId = originalWorkout.cloudId,
            title = originalWorkout.title,
            content = originalWorkout.content,
            createdAt = originalWorkout.createdAt
        )
        workoutDao.insertWorkout(restoredWorkoutInstance)

        // 4. Verify the saved workout is restored in Room and visible
        val restoredList = workoutDao.getAllSavedWorkouts().first()
        assertEquals(1, restoredList.size)
        assertEquals("Morning Calisthenics", restoredList[0].title)
        assertEquals("3x15 Pushups\n3x20 Squats\n3x10 Pullups", restoredList[0].content)
        assertEquals("workout-uuid-123", restoredList[0].cloudId)
    }

    @Test
    fun testSavedDietChartLogoutAndRestoreCycle() = runBlocking {
        val dietChartDao = db.savedDietChartDao()

        // 1. User saves a diet chart
        val originalChart = SavedDietChart(
            cloudId = "chart-uuid-456",
            name = "Keto High-Protein Plan",
            chartContent = "Breakfast: Eggs & Avocado\nLunch: Chicken Salad",
            shoppingList = "Eggs, Avocado, Chicken Breast, Olive Oil",
            createdAt = 1700000000000L
        )
        dietChartDao.insertChart(originalChart)

        // Verify it exists
        val beforeLogout = dietChartDao.getAllSavedCharts().first()
        assertEquals(1, beforeLogout.size)
        assertEquals("Keto High-Protein Plan", beforeLogout[0].name)

        // 2. User logs out
        db.clearAllTables()
        assertTrue(dietChartDao.getAllSavedCharts().first().isEmpty())

        // 3. User logs back in -> restored via deserialization
        val constructor = SavedDietChart::class.java.getDeclaredConstructor()
        val restoredChart = constructor.newInstance().copy(
            cloudId = originalChart.cloudId,
            name = originalChart.name,
            chartContent = originalChart.chartContent,
            shoppingList = originalChart.shoppingList,
            createdAt = originalChart.createdAt
        )
        dietChartDao.insertChart(restoredChart)

        // 4. Verify diet chart is restored
        val restoredList = dietChartDao.getAllSavedCharts().first()
        assertEquals(1, restoredList.size)
        assertEquals("Keto High-Protein Plan", restoredList[0].name)
        assertEquals("Eggs, Avocado, Chicken Breast, Olive Oil", restoredList[0].shoppingList)
    }

    @Test
    fun testSavedChatLogoutAndRestoreCycle() = runBlocking {
        val chatDao = db.savedChatDao()

        // 1. User saves a chat
        val originalChat = SavedChat(
            cloudId = "chat-uuid-789",
            title = "Knee Pain Rehabilitation Plan",
            messages = """[{"isUser":true,"message":"My knee hurts when squatting"},{"isUser":false,"message":"Avoid deep flexion and focus on glute bridges."}]""",
            createdAt = 1700000000000L
        )
        chatDao.insertChat(originalChat)

        // Verify it exists
        val beforeLogout = chatDao.getAllSavedChats().first()
        assertEquals(1, beforeLogout.size)
        assertEquals("Knee Pain Rehabilitation Plan", beforeLogout[0].title)

        // 2. User logs out
        db.clearAllTables()
        assertTrue(chatDao.getAllSavedChats().first().isEmpty())

        // 3. User logs back in -> restored via deserialization
        val constructor = SavedChat::class.java.getDeclaredConstructor()
        val restoredChat = constructor.newInstance().copy(
            cloudId = originalChat.cloudId,
            title = originalChat.title,
            messages = originalChat.messages,
            createdAt = originalChat.createdAt
        )
        chatDao.insertChat(restoredChat)

        // 4. Verify chat is restored
        val restoredList = chatDao.getAllSavedChats().first()
        assertEquals(1, restoredList.size)
        assertEquals("Knee Pain Rehabilitation Plan", restoredList[0].title)
        assertTrue(restoredList[0].messages.contains("My knee hurts"))
    }
}
