package com.example.presentation.common

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app is dark-only and uses theme tokens (MaterialTheme.colorScheme, AccentTokens), never the old light-mode literals or `if (isDark) ... else ...`
 * branches. This fails the build if a screen brings them back. [pendingCleanup] lists the files still to be cleaned; shrink it, never grow it.
 */
class DesignTokenGuardTest {

    private fun sourceFiles(): List<File> {
        val root = listOf("src/main/java/com/example", "app/src/main/java/com/example").map { File(it) }.firstOrNull { it.exists() }
        assertTrue("source folder not found (working dir ${File(".").absolutePath})", root != null)
        return root!!.walkTopDown().filter { it.isFile && it.extension == "kt" }
            .filter { !it.path.replace('\\', '/').contains("/ui/theme/") }.toList()
    }

    private fun code(file: File): String = file.readLines()
        .filterNot { it.trim().startsWith("*") || it.trim().startsWith("/*") || it.trim().startsWith("//") }
        .joinToString("\n")

    private val oldStyle = Regex("""\b(Emerald\d*|Slate\d*|Orange\d+|Red500|TextPrimary)\b|Color\.White|Color\.Black|Color\(0x|isDark\) .* else|if \(isDark\) \{""")

    /** Camera overlays and image scrims need plain white and black: these files may use Color.White and Color.Black (nothing else old-style). */
    private val mayUsePlainWhiteAndBlack = setOf("ScannerScreen.kt", "QrScannerScreen.kt", "UniversalAssistantScreen.kt")
    private val oldStyleWithoutPlain = Regex("""\b(Emerald\d*|Slate\d*|Orange\d+|Red500|TextPrimary)\b|Color\(0x|isDark\) .* else|if \(isDark\) \{""")

    private fun hasOldStyle(file: File): Boolean =
        (if (file.name in mayUsePlainWhiteAndBlack) oldStyleWithoutPlain else oldStyle).containsMatchIn(code(file))

    /** Files that still contain old-style colors or light-mode branches. Remove a file from this list when it is cleaned. */
    private val pendingCleanup = setOf("EditFocusScreen.kt", "BadgeGalleryScreen.kt", "LifestyleScreen.kt", "HealthGoalsScreen.kt")

    @Test
    fun screensUseThemeTokensOnly() {
        val offenders = sourceFiles().filter { it.name !in pendingCleanup }.filter { hasOldStyle(it) }.map { it.name }.sorted()
        assertEquals("use MaterialTheme.colorScheme tokens (dark-only), no old light-mode colors or isDark branches, in: $offenders", emptyList<String>(), offenders)
    }

    @Test
    fun theCleanupListOnlyContainsFilesThatStillNeedIt() {
        val stale = sourceFiles().filter { it.name in pendingCleanup }.filter { !hasOldStyle(it) }.map { it.name }
        assertEquals("these files are clean now: remove them from pendingCleanup: $stale", emptyList<String>(), stale)
    }
}
