package com.example.ui.theme

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source checks that the theme choice is wired through the app: the profile field, the database migration, the root theme and the picker. */
class ThemeWiringTest {

    private fun source(path: String): String {
        val root = listOf("src/main/java/com/example", "app/src/main/java/com/example").map { File(it) }.first { it.exists() }
        return File(root, path).readLines().filterNot { it.trim().startsWith("//") || it.trim().startsWith("*") }.joinToString("\n")
    }

    @Test
    fun theProfileFieldDefaultsToTheDefaultPalette() {
        assertTrue(source("data/local/Entities.kt").contains("val themeId: String = \"${ThemePalettes.DEFAULT_ID}\""))
        assertEquals("chartreuse", ThemePalettes.DEFAULT_ID)
    }

    @Test
    fun theDatabaseMigratesTheNewColumnWithoutLosingData() {
        val db = source("data/local/AppDatabase.kt")
        assertTrue(db.contains("version = 54"))
        assertTrue(db.contains("MIGRATION_52_53 = object : Migration(52, 53)"))
        assertTrue(db.contains("ALTER TABLE user_profile ADD COLUMN themeId TEXT NOT NULL DEFAULT '${ThemePalettes.DEFAULT_ID}'"))
        assertTrue("the migration must be registered", db.contains("MIGRATION_52_53, MIGRATION_53_54)"))
    }

    @Test
    fun theAppStartsInTheRememberedPaletteAndFollowsTheProfile() {
        val main = source("MainActivity.kt")
        assertTrue(main.contains("ThemeCache.load"))
        assertTrue(main.contains("MyApplicationTheme(darkTheme = true, palette = palette)"))
        assertTrue(main.contains("ThemePalettes.byId(it.themeId)"))
        assertTrue(main.contains("\"appearance\""))
    }

    @Test
    fun thePickerShowsEveryPaletteAndSavesThroughTheViewModel() {
        val screen = source("presentation/settings/AppearanceScreen.kt")
        assertTrue(screen.contains("ThemePalettes.all"))
        assertTrue(screen.contains("viewModel.setTheme(palette.id)"))
        assertTrue(source("presentation/viewmodel/ShasthoViewModel.kt").contains("fun setTheme(themeId: String)"))
    }

    @Test
    fun theThemeEngineKeepsThePaletteInStep() {
        val theme = source("ui/theme/Theme.kt")
        assertTrue(theme.contains("AppPalette.current = palette"))
        assertTrue(theme.contains("colorSchemeFor(palette)"))
        assertTrue("dialogs and sheets use the palette too", theme.contains("surfaceContainerHigh = Color(p.surfaceVariant)"))
    }
}
