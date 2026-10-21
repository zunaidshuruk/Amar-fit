package com.example.data.health

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source checks for the BMI fix (docs/bmi-and-stale-state-audit.md): the calculator saves, the logic exists once, the Weight log follows the profile. */
class BmiWiringTest {
    private fun source(path: String): String {
        val root = listOf("src/main/java/com/example", "app/src/main/java/com/example").map { File(it) }.first { it.exists() }
        return File(root, path).readLines().filterNot { it.trim().startsWith("//") || it.trim().startsWith("*") }.joinToString("\n")
    }

    @Test
    fun theBmiCalculatorOnTheHealthTabCanSaveItsNumbers() {
        val health = source("presentation/health/HealthScreen.kt")
        val dialog = health.substringAfter("if (showBmiDialog)")
        assertTrue("Save calls the same function as the Weight log", dialog.contains("viewModel.setWeightAndHeight(w, hCm)"))
        assertTrue(dialog.contains("Text(\"Save\")"))
        assertFalse("the dialog no longer ends with a Close that throws the change away", dialog.contains("confirmButton = {\n                TextButton(onClick = { showBmiDialog = false })"))
    }

    @Test
    fun theBmiFormulaExistsOnlyOnce() {
        val inline = Regex("""\w*[hH]eight\w*\s*\*\s*\w*[hH]eight\w*""")
        val offenders = File(listOf("src/main/java/com/example/presentation", "app/src/main/java/com/example/presentation").first { File(it).exists() })
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.filter { f ->
                f.readLines().filterNot { it.trim().startsWith("//") || it.trim().startsWith("*") }.any { inline.containsMatchIn(it) }
            }.map { it.name }.toList()
        assertTrue("screens must use BodyMetrics instead of writing height x height: $offenders", offenders.isEmpty())
        for (screen in listOf("presentation/health/HealthScreen.kt", "presentation/metrics/WeightLogScreen.kt", "presentation/profile/ProfileScreen.kt")) {
            assertTrue("$screen uses BodyMetrics.bmi", source(screen).contains("BodyMetrics.bmi("))
        }
    }

    @Test
    fun theWeightLogFollowsTheRealProfileAndRefusesToSaveBeforeItIsLoaded() {
        val log = source("presentation/metrics/WeightLogScreen.kt")
        assertTrue(log.contains("LaunchedEffect(profile?.weightKg)"))
        assertTrue(log.contains("LaunchedEffect(profile?.heightCm)"))
        assertTrue(log.contains("if (profile == null)"))
        assertTrue(log.contains("BodyMetrics.isValid(w, h)"))
    }

    @Test
    fun thePickerKeepsTheExactValueWhenNothingWasMoved() {
        val picker = source("ui/components/HeightWeightPickerDialog.kt")
        assertTrue(picker.contains("onConfirm(if (untouched) initialValue else finalCanonical)"))
        assertFalse("no more cutting off with toInt() in the conversions", picker.contains("2.20462f).toInt()") || picker.contains("/ 2.54f).toInt()"))
    }
}
