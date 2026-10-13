package com.example.data.cycle

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards for the promise that cycle data never reaches friends, the leaderboard, challenges or the activity feed:
 * the functions that write those must not mention the cycle, and the cycle code must not call them.
 */
class CycleIsolationTest {

    private fun source(path: String): String {
        val candidates = listOf("src/main/java/$path", "app/src/main/java/$path")
        val file = candidates.map { File(it) }.firstOrNull { it.exists() }
        assertTrue("source file not found: $path (working dir ${File(".").absolutePath})", file != null)
        return file!!.readText()
    }

    /** The text of one function: from its declaration to the next top-level function of the same object. */
    private fun functionBody(text: String, name: String): String {
        val start = text.indexOf("fun $name(")
        assertTrue("function $name not found", start >= 0)
        val next = text.indexOf("\n    suspend fun ", start + 10).let { a ->
            val b = text.indexOf("\n    fun ", start + 10)
            listOf(a, b).filter { it > 0 }.minOrNull() ?: text.length
        }
        return text.substring(start, next)
    }

    @Test
    fun friendAndLeaderboardWritersNeverMentionTheCycle() {
        val firebase = source("com/example/data/repository/FirebaseManager.kt")
        for (name in listOf("syncFriendStats", "syncPublicProfile", "postActivityEvent", "createChallenge", "sendKudos")) {
            if (!firebase.contains("fun $name(")) continue
            assertFalse("$name must not touch cycle data", functionBody(firebase, name).lowercase().contains("cycle"))
        }
    }

    @Test
    fun cycleCodeNeverCallsTheSocialWriters() {
        val cycleFiles = listOf(
            "com/example/data/cycle/CycleModels.kt", "com/example/data/cycle/CyclePredictor.kt", "com/example/data/cycle/CycleNotices.kt",
            "com/example/data/cycle/CycleAiSummary.kt", "com/example/data/cycle/CycleTexts.kt", "com/example/presentation/cycle/CycleScreen.kt",
            "com/example/presentation/cycle/CycleCards.kt", "com/example/presentation/cycle/CycleSettingsScreen.kt"
        )
        for (file in cycleFiles) {
            val text = source(file)
            for (social in listOf("syncFriendStats", "syncPublicProfile", "postActivityEvent", "createChallenge", "sendKudos", "friend_stats", "public_profiles")) {
                assertFalse("$file must not use $social", text.contains(social))
            }
        }
    }

    @Test
    fun theAiSummaryHelperNeverReadsDailyLogs() {
        // code only: the comments are allowed to say what is NOT sent
        val code = source("com/example/data/cycle/CycleAiSummary.kt").lines()
            .filterNot { it.trim().startsWith("*") || it.trim().startsWith("/*") || it.trim().startsWith("//") }
            .joinToString("\n")
        assertFalse(code.contains("CycleDayLog"))
        assertFalse(code.lowercase().contains("symptom"))
        assertFalse(code.lowercase().contains("mood"))
    }
}
