package com.example.accessibilitymanager

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityOrderingTest {
    @Test
    fun movingItemUpChangesOrder() {
        val values = mutableListOf("talkback", "switch", "select")
        val moved = values.removeAt(2)
        values.add(1, moved)
        assertEquals(listOf("talkback", "select", "switch"), values)
    }

    @Test
    fun movingItemDownChangesOrder() {
        val values = mutableListOf("talkback", "switch", "select")
        val moved = values.removeAt(0)
        values.add(1, moved)
        assertEquals(listOf("switch", "talkback", "select"), values)
    }
}
