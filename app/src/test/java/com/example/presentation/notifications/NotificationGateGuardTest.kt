package com.example.presentation.notifications

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Allow KardIQ notifications" must switch EVERYTHING off. Every notification is posted from NotificationHelper or MedicineNotifier, and each
 * post must first check NotificationGate. These tests fail if a notification is posted anywhere else, or without the check.
 */
class NotificationGateGuardTest {

    private fun sourceFiles(): List<File> {
        val root = listOf("src/main/java/com/example", "app/src/main/java/com/example").map { File(it) }.firstOrNull { it.exists() }
        assertTrue("source folder not found (working dir ${File(".").absolutePath})", root != null)
        return root!!.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun code(file: File): String = file.readLines()
        .filterNot { it.trim().startsWith("*") || it.trim().startsWith("/*") || it.trim().startsWith("//") }
        .joinToString("\n")

    private val posts = Regex("""\bnotify\(""")
    private val allowed = setOf("NotificationHelper.kt", "MedicineNotifier.kt")

    @Test
    fun notificationsArePostedOnlyFromTheTwoGatedClasses() {
        val offenders = sourceFiles().filter { it.name !in allowed }.filter { posts.containsMatchIn(code(it)) }.map { it.name }
        assertEquals("post notifications through NotificationHelper (or MedicineNotifier), not directly, in: $offenders", emptyList<String>(), offenders)
    }

    @Test
    fun everyPostChecksTheMasterSwitchFirst() {
        for (file in sourceFiles().filter { it.name in allowed }) {
            val text = code(file)
            val postCount = posts.findAll(text).count()
            val gateCount = Regex("""NotificationGate\.isEnabled""").findAll(text).count()
            assertTrue("${file.name}: $postCount notifications but only $gateCount master switch checks", gateCount >= postCount)
        }
    }

    @Test
    fun theSchedulerKeepsTheGateInSyncAndClearsPendingAlertsWhenTurnedOff() {
        val scheduler = code(sourceFiles().first { it.name == "ReminderScheduler.kt" })
        assertTrue(scheduler.contains("NotificationGate.setEnabled(context, prefs.masterEnabled)"))
        assertTrue("pending snoozes are cancelled", scheduler.contains("MedicineNotifier.cancelSnooze"))
        assertTrue("pending missed-dose checks are cancelled", scheduler.contains("MedicineNotifier.cancelMissedCheck"))
        assertTrue("what is showing is cleared", scheduler.contains("cancelAll()"))
    }
}
