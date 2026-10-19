package com.example.security

import com.example.presentation.navigation.NavTabs
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards from the security review (docs/security-review.md). They fail the build if a fixed weakness comes back:
 * a new exported component, Android backup switched on, a token or key written to the log, a screen opened from any app's intent without a check,
 * a debuggable release build, or a secret committed to the repository.
 */
class SecurityGuardTest {

    private fun appDir(): File = listOf(File("."), File("app")).first { File(it, "src/main/AndroidManifest.xml").exists() }
    private fun repoRoot(): File = listOf(File(".."), File("."), File("..").absoluteFile.parentFile!!).first { File(it, "settings.gradle.kts").exists() }
    private fun manifest(): String = File(appDir(), "src/main/AndroidManifest.xml").readText()
    private fun mainSources(): List<File> =
        File(appDir(), "src/main/java").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun codeOf(file: File): String = file.readLines()
        .filterNot { it.trim().startsWith("*") || it.trim().startsWith("/*") || it.trim().startsWith("//") }.joinToString("\n")

    @Test
    fun onlyTheExpectedComponentsAreExportedToOtherApps() {
        val component = Regex("""<(activity|activity-alias|service|receiver|provider)\b([^>]*)>""", RegexOption.DOT_MATCHES_ALL)
        val exported = component.findAll(manifest()).filter { it.groupValues[2].contains("""android:exported="true"""") }
            .map { Regex("""android:name="([^"]+)"""").find(it.groupValues[2])!!.groupValues[1].substringAfterLast('.') }.toSet()
        assertEquals(
            "a new exported component needs a security review (add it here only after one): $exported",
            setOf("MainActivity", "PrivacyPolicyActivity", "ViewPermissionUsageActivity", "MusicNotificationListenerService"), exported
        )
    }

    @Test
    fun theBootReceiverIsNotExported() {
        val boot = Regex("""<receiver[^>]*BootReceiver[^>]*>""", RegexOption.DOT_MATCHES_ALL).find(manifest())!!.value
        assertTrue(boot.contains("""android:exported="false""""))
    }

    @Test
    fun androidBackupOfHealthDataIsOff() {
        assertTrue(manifest().contains("""android:allowBackup="false""""))
        val rules = File(appDir(), "src/main/res/xml/data_extraction_rules.xml").readText()
        assertTrue("device transfer excludes the database too", rules.contains("<device-transfer>") && rules.contains("""<exclude domain="database""""))
        assertTrue(File(appDir(), "src/main/res/xml/backup_rules.xml").readText().contains("""<exclude domain="sharedpref""""))
    }

    @Test
    fun noTokenPasswordOrKeyIsWrittenToTheLog() {
        val secretInLog = Regex("""Log\.[dviwe]\([^\n]*(\$\{?(token|idToken|password|apiKey|secret|fcmToken)\b|\+\s*(token|idToken|password|apiKey|secret|fcmToken)\b)""", RegexOption.IGNORE_CASE)
        val offenders = mainSources().filter { secretInLog.containsMatchIn(codeOf(it)) }.map { it.name }
        assertEquals("these files log a token, password or key: $offenders", emptyList<String>(), offenders)
    }

    @Test
    fun aScreenOpenedFromAnIntentIsAlwaysCheckedAgainstTheAllowList() {
        val main = codeOf(File(appDir(), "src/main/java/com/example/MainActivity.kt"))
        val reads = Regex("""getStringExtra\("NAVIGATE_TO"\)""").findAll(main).count()
        val checks = Regex("""takeIf \{ NavTabs\.isAllowedNotificationRoute\(it\) \}""").findAll(main).count()
        assertTrue("MainActivity reads NAVIGATE_TO", reads >= 1)
        assertEquals("every read of NAVIGATE_TO must go through the allow-list", reads, checks)
    }

    @Test
    fun theReleaseBuildIsNotDebuggable() {
        val gradle = File(appDir(), "build.gradle.kts").readText()
        val release = gradle.substringAfter("release {").substringBefore("\n    }")
        assertFalse(release.contains("isDebuggable = true"))
    }

    @Test
    fun noSecretIsCommittedToTheRepository() {
        // patterns are assembled from pieces so this file does not match itself
        val patterns = mapOf(
            "Google API key" to Regex("AIza" + "[0-9A-Za-z_\\-]{35}"),
            "private key block" to Regex("-----BEGIN [A-Z ]*PRIVATE" + " KEY-----"),
            "GitHub token" to Regex("gh[pousr]_" + "[A-Za-z0-9]{36}"),
            "AWS access key id" to Regex("AK" + "IA[0-9A-Z]{16}"),
            "service account key" to Regex("\"private_" + "key\"\\s*:")
        )
        val skipDirs = setOf(".git", "build", ".gradle", "node_modules", ".idea")
        val offenders = ArrayList<String>()
        repoRoot().walkTopDown().onEnter { it.name !in skipDirs }.filter { it.isFile && it.extension in setOf("kt", "kts", "xml", "json", "yml", "yaml", "js", "md", "properties", "txt", "py", "sh", "gradle") }.forEach { file ->
            val text = file.readText()
            for ((name, pattern) in patterns) {
                // the Firebase client key in strings.xml is public by design (it ships in every APK); it is restricted in Google Cloud instead
                if (name == "Google API key" && file.path.replace('\\', '/').endsWith("res/values/strings.xml")) continue
                if (pattern.containsMatchIn(text)) offenders.add("${file.name}: $name")
            }
        }
        assertEquals("secrets found in the repository: $offenders", emptyList<String>(), offenders)
    }

    @Test
    fun onlyKnownScreensCanBeOpenedFromANotificationLink() {
        for (route in listOf("today", "settings", "friends", "medicine_history", "dm/abc_def/Amina", "dm/UID1_UID2/")) {
            assertTrue(route, NavTabs.isAllowedNotificationRoute(route))
        }
        for (route in listOf(null, "", "admin", "../settings", "settings/../x", "dm/a_b/c/d", "dm//x", "dm/a b/x", "dm/" + "a".repeat(101) + "/x",
            "javascript:alert(1)", "http://evil.example", "profile", "appearance")) {
            assertFalse("$route", NavTabs.isAllowedNotificationRoute(route))
        }
    }
}
