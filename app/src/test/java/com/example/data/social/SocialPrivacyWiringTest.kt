package com.example.data.social

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source checks that the two privacy switches are wired from the Settings row to Firebase and to the database. */
class SocialPrivacyWiringTest {

    private fun source(path: String): String {
        val root = listOf("src/main/java/com/example", "app/src/main/java/com/example").map { File(it) }.first { it.exists() }
        return File(root, path).readLines().filterNot { it.trim().startsWith("//") || it.trim().startsWith("*") }.joinToString("\n")
    }

    @Test
    fun theProfileKeepsBothSwitchesAndTheDatabaseMigratesThem() {
        val entities = source("data/local/Entities.kt")
        assertTrue(entities.contains("val showOnLeaderboard: Boolean = true"))
        assertTrue(entities.contains("val shareProgressWithFriends: Boolean = true"))
        val db = source("data/local/AppDatabase.kt")
        assertTrue(db.contains("MIGRATION_53_54 = object : Migration(53, 54)"))
        assertTrue(db.contains("ALTER TABLE user_profile ADD COLUMN showOnLeaderboard INTEGER NOT NULL DEFAULT 1"))
        assertTrue(db.contains("ALTER TABLE user_profile ADD COLUMN shareProgressWithFriends INTEGER NOT NULL DEFAULT 1"))
    }

    @Test
    fun theLeaderboardPublishesTheFlagAndLeavesHiddenPeopleOut() {
        val firebase = source("data/repository/FirebaseManager.kt")
        assertTrue(firebase.contains("\"leaderboardVisible\" to leaderboardVisible"))
        assertTrue(firebase.contains("SocialPrivacy.visibleTop"))
        assertTrue(source("data/repository/AppRepository.kt").contains("leaderboardVisible = profileToSave.showOnLeaderboard"))
    }

    @Test
    fun progressIsOnlyWrittenWhileSharingIsOnAndIsDeletedWhenSwitchedOff() {
        val repo = source("data/repository/AppRepository.kt")
        assertTrue(repo.contains("currentProfile.shareProgressWithFriends && date =="))
        assertTrue(repo.contains("ProgressChange.REMOVE -> FirebaseManager.deleteFriendStats()"))
        assertTrue(source("data/repository/FirebaseManager.kt").contains("suspend fun deleteFriendStats()"))
    }

    @Test
    fun settingsHasBothSwitchesAndTheFriendCardExplainsMissingProgress() {
        val settings = source("presentation/settings/SettingsScreen.kt")
        assertTrue(settings.contains("viewModel.setShowOnLeaderboard(it)"))
        assertTrue(settings.contains("viewModel.setShareProgressWithFriends(it)"))
        val friends = source("presentation/social/FriendsScreen.kt")
        assertTrue(friends.contains("is not sharing their daily progress"))
        assertTrue("never 'Loading' forever", friends.contains("stats == null && !friendStatsMissing"))
    }
}
