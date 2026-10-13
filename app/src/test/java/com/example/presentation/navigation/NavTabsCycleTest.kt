package com.example.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavTabsCycleTest {

    @Test
    fun cycleIsATabThatCanBeChosenForTheBar() {
        assertTrue("cycle" in NavTabs.CHOOSABLE_ROUTES)
        assertTrue(NavTabs.isTabRoute("cycle"))
        // it has its own header, so no greeting top bar
        assertFalse(NavTabs.showsTopBar("cycle"))
    }

    @Test
    fun onlyProfilesThatCanUseTheTrackerSeeTheTab() {
        assertTrue("cycle" in NavTabs.chooseable(true))
        assertFalse("cycle" in NavTabs.chooseable(false))
        assertFalse("cycle" in NavTabs.hidden(listOf("fitness"), cycleAllowed = false))
        assertTrue("cycle" in NavTabs.hidden(listOf("fitness"), cycleAllowed = true))
    }

    @Test
    fun aSavedBarKeepsCycleOnlyWhileAllowed() {
        assertEquals(listOf("fitness", "cycle", "health"), NavTabs.parse("fitness,cycle,health", cycleAllowed = true))
        // gender changed away from Female: the tab disappears from the bar, the others stay in order
        assertEquals(listOf("fitness", "health"), NavTabs.parse("fitness,cycle,health", cycleAllowed = false))
        // nothing left that is allowed: the default bar
        assertEquals(NavTabs.DEFAULT_BAR, NavTabs.parse("cycle", cycleAllowed = false))
    }

    @Test
    fun theBarStillHoldsThreeTabsAndCycleCanReplaceOne() {
        var bar = listOf("fitness", "nutrition", "health")
        assertEquals(bar, NavTabs.toggle(bar, "cycle")) // full bar: remove one first
        bar = NavTabs.toggle(bar, "nutrition")
        bar = NavTabs.toggle(bar, "cycle")
        assertEquals(listOf("fitness", "health", "cycle"), bar)
    }

    @Test
    fun defaultsAreUnchangedAndDoNotIncludeCycle() {
        assertEquals(listOf("fitness", "nutrition", "health"), NavTabs.DEFAULT_BAR)
    }
}
