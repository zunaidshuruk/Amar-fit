package com.example.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavTabsTest {

    @Test
    fun testParseDefaults() {
        assertEquals(NavTabs.DEFAULT_BAR, NavTabs.parse(null))
        assertEquals(NavTabs.DEFAULT_BAR, NavTabs.parse(""))
        assertEquals(NavTabs.DEFAULT_BAR, NavTabs.parse("junk,foo"))
    }

    @Test
    fun testParseDuplicatesAndLimits() {
        val result = NavTabs.parse("sleep, FRIENDS ,sleep,health,nutrition,fitness")
        assertEquals(listOf("sleep", "friends", "health"), result)
    }

    @Test
    fun testToCsv() {
        assertEquals("sleep,health", NavTabs.toCsv(listOf("sleep", "health")))
    }

    @Test
    fun testHidden() {
        assertEquals(listOf("sleep", "friends", "settings"), NavTabs.hidden(NavTabs.DEFAULT_BAR))
    }

    @Test
    fun testToggle() {
        assertEquals(listOf("nutrition", "health"), NavTabs.toggle(NavTabs.DEFAULT_BAR, "fitness"))
        assertEquals(NavTabs.DEFAULT_BAR, NavTabs.toggle(NavTabs.DEFAULT_BAR, "sleep"))
        assertEquals(listOf("fitness"), NavTabs.toggle(listOf("fitness"), "fitness"))
        assertEquals(listOf("fitness", "sleep"), NavTabs.toggle(listOf("fitness"), "sleep"))
        assertEquals(NavTabs.DEFAULT_BAR, NavTabs.toggle(NavTabs.DEFAULT_BAR, "unknown"))
    }

    @Test
    fun testMoveUpAndDown() {
        val initial = listOf("fitness", "nutrition", "health")
        assertEquals(listOf("fitness", "health", "nutrition"), NavTabs.moveUp(initial, "health"))
        assertEquals(initial, NavTabs.moveUp(initial, "fitness"))
        assertEquals(initial, NavTabs.moveDown(initial, "health"))
        assertEquals(listOf("nutrition", "fitness", "health"), NavTabs.moveDown(initial, "fitness"))
        assertEquals(initial, NavTabs.moveUp(initial, "unknown"))
        assertEquals(initial, NavTabs.moveDown(initial, "unknown"))
    }
}
