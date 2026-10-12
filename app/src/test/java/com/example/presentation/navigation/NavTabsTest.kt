package com.example.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavTabsTest {

    @Test
    fun testParse() {
        assertEquals(NavTabs.DEFAULT_BAR, NavTabs.parse(null))
        assertEquals(NavTabs.DEFAULT_BAR, NavTabs.parse(""))
        assertEquals(NavTabs.DEFAULT_BAR, NavTabs.parse("junk,foo"))
        assertEquals(listOf("sleep", "friends", "health"), NavTabs.parse("sleep, FRIENDS ,sleep,health,nutrition,fitness"))
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
        assertEquals(NavTabs.DEFAULT_BAR, NavTabs.toggle(NavTabs.DEFAULT_BAR, "sleep")) // bar full
        assertEquals(listOf("fitness"), NavTabs.toggle(listOf("fitness"), "fitness")) // never empty
        assertEquals(listOf("fitness", "sleep"), NavTabs.toggle(listOf("fitness"), "sleep"))
        assertEquals(listOf("fitness"), NavTabs.toggle(listOf("fitness"), "unknown"))
    }

    @Test
    fun testMove() {
        val bar = listOf("fitness", "nutrition", "health")
        assertEquals(listOf("fitness", "health", "nutrition"), NavTabs.moveUp(bar, "health"))
        assertEquals(bar, NavTabs.moveUp(bar, "fitness")) // first item
        assertEquals(listOf("nutrition", "fitness", "health"), NavTabs.moveDown(bar, "fitness"))
        assertEquals(bar, NavTabs.moveDown(bar, "health")) // last item
    }
}
