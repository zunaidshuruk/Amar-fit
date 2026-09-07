package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.LibraryExercise
import com.example.data.model.WorkoutExercise
import com.example.data.model.WorkoutPlan
import com.example.data.remote.RetrofitClient
import com.squareup.moshi.Types
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ExerciseLibraryTest {

    @Test
    fun testExercisesJsonAssetParseable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val json = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        assertTrue(json.isNotBlank())

        val listType = Types.newParameterizedType(List::class.java, LibraryExercise::class.java)
        val adapter = RetrofitClient.moshi.adapter<List<LibraryExercise>>(listType)
        val exercises = adapter.fromJson(json)

        assertNotNull(exercises)
        assertTrue(exercises!!.isNotEmpty())
        assertEquals(876, exercises.size)

        val first = exercises.first()
        assertTrue(first.id.isNotBlank())
        assertTrue(first.name.isNotBlank())
        assertTrue(first.category.isNotBlank())
    }

    @Test
    fun testExerciseFilteringLogic() {
        val exercises = listOf(
            LibraryExercise(
                id = "ex1",
                name = "Bench Press",
                category = "strength",
                equipment = "barbell",
                primaryMuscles = listOf("chest"),
                level = "intermediate"
            ),
            LibraryExercise(
                id = "ex2",
                name = "Incline Dumbbell Press",
                category = "strength",
                equipment = "dumbbell",
                primaryMuscles = listOf("chest"),
                level = "intermediate"
            ),
            LibraryExercise(
                id = "ex3",
                name = "Barbell Squat",
                category = "strength",
                equipment = "barbell",
                primaryMuscles = listOf("quadriceps"),
                level = "expert"
            )
        )

        // Filter by search substring
        val searchFiltered = exercises.filter { it.name.lowercase().contains("press") }
        assertEquals(2, searchFiltered.size)

        // Filter by equipment
        val eqFiltered = exercises.filter { it.equipment.equals("barbell", ignoreCase = true) }
        assertEquals(2, eqFiltered.size)

        // Combined AND filter
        val combined = exercises.filter {
            it.name.lowercase().contains("press") &&
            it.equipment.equals("barbell", ignoreCase = true) &&
            it.primaryMuscles.any { m -> m.equals("chest", ignoreCase = true) }
        }
        assertEquals(1, combined.size)
        assertEquals("ex1", combined[0].id)
    }

    @Test
    fun testWorkoutPlanCreationFromSelectedExercises() {
        val selected = listOf(
            LibraryExercise(id = "1", name = "Push Up"),
            LibraryExercise(id = "2", name = "Pull Up")
        )

        val mainExercises = selected.map { ex ->
            WorkoutExercise(
                name = ex.name,
                sets = 3,
                reps = "10",
                durationSeconds = 0,
                restSeconds = 30,
                youtubeSearchQuery = ex.name,
                metValue = 3.5
            )
        }

        val plan = WorkoutPlan(
            title = "Chest & Back Day",
            warmup = emptyList(),
            mainExercises = mainExercises,
            cooldown = emptyList()
        )

        val planAdapter = RetrofitClient.moshi.adapter(WorkoutPlan::class.java)
        val json = planAdapter.toJson(plan)
        assertNotNull(json)
        assertTrue(json.contains("Chest & Back Day"))
        assertTrue(json.contains("Push Up"))
        assertTrue(json.contains("Pull Up"))

        val restored = planAdapter.fromJson(json)
        assertNotNull(restored)
        assertEquals("Chest & Back Day", restored!!.title)
        assertEquals(2, restored.mainExercises.size)
        assertEquals(3, restored.mainExercises[0].sets)
        assertEquals("10", restored.mainExercises[0].reps)
        assertEquals(30, restored.mainExercises[0].restSeconds)
    }
}
