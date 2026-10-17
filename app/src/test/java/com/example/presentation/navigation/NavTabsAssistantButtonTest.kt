package com.example.presentation.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavTabsAssistantButtonTest {

    @Test
    fun theButtonIsNeverShownBeforeLogin() {
        for (route in listOf("auth", "onboarding", "privacy_consent")) assertFalse(route, NavTabs.showsAssistantButton(route))
        assertFalse("nothing is known yet while the app starts", NavTabs.showsAssistantButton(null))
    }

    @Test
    fun theButtonNeverCoversAnInputBarOrABottomAction() {
        assertFalse("chat: it would sit over the send button", NavTabs.showsAssistantButton("dm/a_b/Amina"))
        assertFalse("exercise library: it has a bottom action bar", NavTabs.showsAssistantButton("exercise_library"))
        assertFalse(NavTabs.showsAssistantButton("scanner"))
        assertFalse(NavTabs.showsAssistantButton("foodlog"))
        assertFalse(NavTabs.showsAssistantButton("cycle"))
        assertFalse(NavTabs.showsAssistantButton("cycle_settings"))
    }

    @Test
    fun theButtonIsNotShownOnTheAssistantItselfOrOnLongTextPages() {
        assertFalse(NavTabs.showsAssistantButton("universal_assistant"))
        assertFalse(NavTabs.showsAssistantButton("universal_assistant?starter={starter}&fresh={fresh}"))
        assertFalse(NavTabs.showsAssistantButton("privacy_policy"))
        assertFalse(NavTabs.showsAssistantButton("terms_of_service"))
    }

    @Test
    fun theButtonIsShownOnTheTabsAndTheOrdinaryPages() {
        assertTrue(NavTabs.showsAssistantButton("today"))
        for (tab in NavTabs.CHOOSABLE_ROUTES.filter { it != "cycle" }) assertTrue(tab, NavTabs.showsAssistantButton(tab))
        for (route in listOf("mealplan", "dietplan", "weightlog", "glucoselog", "profile", "badges", "friends", "leaderboard", "medicine_history", "edit_focus", "health_goals")) {
            assertTrue(route, NavTabs.showsAssistantButton(route))
        }
    }
}
