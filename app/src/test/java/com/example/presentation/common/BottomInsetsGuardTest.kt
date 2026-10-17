package com.example.presentation.common

import com.example.presentation.navigation.NavTabs
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards for "the button is cut off at the bottom". The floating tab bar is drawn at the very bottom of the screen, so it has to keep clear of the system
 * navigation bar itself (gesture pill, three buttons, or buttons at the side in landscape); and the assistant button must never sit over a bottom bar.
 */
class BottomInsetsGuardTest {

    private fun sourceRoot(): File {
        val root = listOf("src/main/java/com/example", "app/src/main/java/com/example").map { File(it) }.firstOrNull { it.exists() }
        assertTrue("source folder not found (working dir ${File(".").absolutePath})", root != null)
        return root!!
    }

    private fun code(file: File): String = file.readLines()
        .filterNot { it.trim().startsWith("*") || it.trim().startsWith("/*") || it.trim().startsWith("//") }
        .joinToString("\n")

    @Test
    fun theFloatingTabBarKeepsClearOfTheSystemNavigationBar() {
        val main = code(File(sourceRoot(), "MainActivity.kt"))
        val start = main.indexOf("bottomBar = {")
        val end = main.indexOf(") { innerPadding ->", start)
        assertTrue("root Scaffold bottomBar not found", start >= 0 && end > start)
        val block = main.substring(start, end)
        assertTrue("the tab bar must use bottomBarSafePadding()", block.contains("bottomBarSafePadding()"))
        assertTrue("the tab bar must tighten on short screens", block.contains("isCompactHeight()"))
    }

    @Test
    fun theAssistantButtonFollowsTheOneRule() {
        val main = code(File(sourceRoot(), "MainActivity.kt"))
        assertTrue(main.contains("NavTabs.showsAssistantButton(currentRoute)"))
        assertFalse("no second, hand-written rule", main.contains("currentRoute !in setOf("))
    }

    @Test
    fun everyScreenWithItsOwnBottomBarHidesTheAssistantButton() {
        // a screen with its own bottom bar (an input bar or an action bar) would have the assistant button on top of it
        val withBottomBar = sourceRoot().walkTopDown().filter { it.isFile && it.extension == "kt" && it.path.replace('\\', '/').contains("/presentation/") }
            .filter { Regex("""\bbottomBar\s*=""").containsMatchIn(code(it)) }.map { it.name }.toSet()
        assertEquals(
            "a new screen with a bottomBar must be added here AND hidden in NavTabs.showsAssistantButton",
            setOf("DirectMessageScreen.kt", "ExerciseLibraryScreen.kt", "UniversalAssistantScreen.kt"), withBottomBar
        )
        assertFalse(NavTabs.showsAssistantButton("dm/a_b/Amina"))
        assertFalse(NavTabs.showsAssistantButton("exercise_library"))
        assertFalse(NavTabs.showsAssistantButton("universal_assistant"))
    }

    @Test
    fun everyScreenCanScrollSoNothingIsCutOffOnASmallPhoneOrWithLargeText() {
        // FitnessScreen is a thin wrapper around the workout screen; QrScannerScreen is a full-screen camera
        val allowedWithoutScrolling = setOf("FitnessScreen.kt", "QrScannerScreen.kt")
        val scrolls = Regex("""verticalScroll\(|LazyColumn\(|LazyVerticalGrid\(|LazyVerticalStaggeredGrid\(|HorizontalPager\(""")
        val offenders = sourceRoot().walkTopDown().filter { it.isFile && it.name.endsWith("Screen.kt") && it.name !in allowedWithoutScrolling }
            .filter { !scrolls.containsMatchIn(code(it)) }.map { it.name }.sorted().toList()
        assertEquals("these screens have no scrolling container (content can be cut off): $offenders", emptyList<String>(), offenders)
    }

    @Test
    fun fullScreenEditorsAreDrawnInsideTheMainWindowNotInAPopUpWindow() {
        // a pop-up window gets its own screen geometry; on some phones that put the Save button partly below the screen edge, under the gesture bar
        val main = code(File(sourceRoot(), "MainActivity.kt"))
        assertTrue("the root hosts the overlay", main.contains("OverlayLayer(overlayHost)"))
        assertTrue("every screen can reach the overlay host", main.contains("LocalOverlayHost provides overlayHost"))
        val insets = code(File(sourceRoot(), "presentation/common/AppInsets.kt"))
        assertTrue("FullScreenDialog draws through the host", insets.contains("LocalOverlayHost.current"))
        assertTrue("the overlay is closed with the back button", insets.contains("BackHandler(onBack = onDismissRequest)"))
    }

    @Test
    fun theBottomOfTheScreenIsNeverLessThanTheMinimumEvenIfThePhoneReportsZero() {
        val insets = code(File(sourceRoot(), "presentation/common/AppInsets.kt"))
        assertTrue(insets.contains("val MinBottomInset: Dp = 20.dp"))
        assertTrue("appSafeInsets includes the guard", insets.contains(".union(bottomGuard())"))
        assertTrue("the gesture area counts too", insets.contains("WindowInsets.safeGestures.only(WindowInsetsSides.Bottom)"))
        val main = code(File(sourceRoot(), "MainActivity.kt"))
        assertTrue("the root Scaffold uses the guarded insets", main.contains("contentWindowInsets = com.example.presentation.common.systemBarsWithFloor()"))
    }
}

