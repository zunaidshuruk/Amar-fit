package com.example.presentation.common

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards for the "button hidden under the gesture bar" bug. A full-screen Dialog is a separate window that, on some phones, does not get the
 * bottom system bar inset, so everything reads the MAIN window's insets through AppInsets.kt instead. These tests fail if someone writes
 * the old pattern again.
 */
class InsetsGuardTest {

    private fun sourceFiles(): List<File> {
        val root = listOf("src/main/java/com/example", "app/src/main/java/com/example").map { File(it) }.firstOrNull { it.exists() }
        assertTrue("source folder not found (working dir ${File(".").absolutePath})", root != null)
        return root!!.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private val allowedToTouchRawInsets = setOf("AppInsets.kt", "Theme.kt")

    private fun code(file: File): String = file.readLines()
        .filterNot { it.trim().startsWith("*") || it.trim().startsWith("/*") || it.trim().startsWith("//") }
        .joinToString("\n")

    @Test
    fun screensAndDialogsReadInsetsOnlyThroughTheSharedHelpers() {
        val raw = Regex("""WindowInsets\.(safeDrawing|safeContent|systemBars|statusBars|navigationBars|displayCutout)|\bnavigationBarsPadding\(\)|\bsystemBarsPadding\(\)|\bstatusBarsPadding\(\)""")
        val offenders = sourceFiles().filter { it.name !in allowedToTouchRawInsets }
            .filter { raw.containsMatchIn(code(it)) }.map { it.name }
        assertEquals("use appSafeInsets(), topSafePadding() or bottomSafePadding() instead of raw insets in: $offenders", emptyList<String>(), offenders)
    }

    @Test
    fun fullScreenDialogsAreBuiltOnlyByTheSharedWrapper() {
        val offenders = sourceFiles().filter { it.name != "AppInsets.kt" }
            .filter { code(it).contains("usePlatformDefaultWidth = false") }.map { it.name }
        assertEquals("use FullScreenDialog { } instead of a raw full-screen Dialog in: $offenders", emptyList<String>(), offenders)
    }

    @Test
    fun theMainWindowInsetsAreProvidedAtTheRoot() {
        val theme = sourceFiles().first { it.name == "Theme.kt" }.readText()
        assertTrue(theme.contains("LocalAppInsets provides"))
    }
}
