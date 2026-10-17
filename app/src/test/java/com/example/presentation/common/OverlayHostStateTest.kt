package com.example.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayHostStateTest {

    @Test
    fun anOpenEditorIsListedAndRemovedWhenItCloses() {
        val host = OverlayHostState()
        val a = Any()
        host.add(OverlayHostState.Entry(a) { })
        assertEquals(1, host.entries.size)
        host.remove(a)
        assertTrue(host.entries.isEmpty())
    }

    @Test
    fun theSameEditorIsNeverListedTwice() {
        val host = OverlayHostState()
        val a = Any()
        host.add(OverlayHostState.Entry(a) { })
        host.add(OverlayHostState.Entry(a) { })
        assertEquals(1, host.entries.size)
    }

    @Test
    fun editorsOpenedLaterAreDrawnOnTop() {
        val host = OverlayHostState()
        val first = Any()
        val second = Any()
        host.add(OverlayHostState.Entry(first) { })
        host.add(OverlayHostState.Entry(second) { })
        assertEquals(listOf(first, second), host.entries.map { it.key })
        host.remove(first)
        assertEquals(listOf(second), host.entries.map { it.key })
    }

    @Test
    fun removingSomethingThatIsNotThereDoesNothing() {
        val host = OverlayHostState()
        host.remove(Any())
        assertTrue(host.entries.isEmpty())
    }
}
