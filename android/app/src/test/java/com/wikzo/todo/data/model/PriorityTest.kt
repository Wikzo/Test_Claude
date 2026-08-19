package com.wikzo.todo.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Priority is the one field with a hand-written wire mapping (a closed
 * lowercase string enum, not Firestore's default enum-name mapping) -- these
 * pin that mapping against /docs/data-model.md so a typo doesn't silently
 * corrupt every task's priority.
 */
class PriorityTest {

    @Test
    fun `firestoreValue matches the documented lowercase strings`() {
        assertEquals("high", Priority.HIGH.firestoreValue)
        assertEquals("medium", Priority.MEDIUM.firestoreValue)
        assertEquals("low", Priority.LOW.firestoreValue)
    }

    @Test
    fun `fromFirestoreValue round-trips every documented value`() {
        assertEquals(Priority.HIGH, Priority.fromFirestoreValue("high"))
        assertEquals(Priority.MEDIUM, Priority.fromFirestoreValue("medium"))
        assertEquals(Priority.LOW, Priority.fromFirestoreValue("low"))
    }

    @Test
    fun `fromFirestoreValue falls back to MEDIUM for unknown or missing values`() {
        assertEquals(Priority.MEDIUM, Priority.fromFirestoreValue(null))
        assertEquals(Priority.MEDIUM, Priority.fromFirestoreValue(""))
        assertEquals(Priority.MEDIUM, Priority.fromFirestoreValue("urgent"))
    }

    @Test
    fun `declaration order doubles as sort order, HIGH before MEDIUM before LOW`() {
        assertEquals(0, Priority.HIGH.ordinal)
        assertEquals(1, Priority.MEDIUM.ordinal)
        assertEquals(2, Priority.LOW.ordinal)
    }
}
