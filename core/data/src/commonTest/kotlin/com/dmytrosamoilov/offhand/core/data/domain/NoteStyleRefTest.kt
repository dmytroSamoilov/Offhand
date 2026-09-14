package com.dmytrosamoilov.offhand.core.data.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class NoteStyleRefTest {

    @Test
    fun `built-in styles keep the legacy preset name as storage key`() {
        assertEquals("MEETING", NoteStyleRef.BuiltIn(NotePreset.MEETING).storageKey())
        assertEquals(NoteStyleRef.BuiltIn(NotePreset.MEETING), NoteStyleRef.fromStorageKey("MEETING"))
    }

    @Test
    fun `custom styles round-trip through the storage key`() {
        val key = NoteStyleRef.Custom(42).storageKey()

        assertEquals("custom:42", key)
        assertEquals(NoteStyleRef.Custom(42), NoteStyleRef.fromStorageKey(key))
    }

    @Test
    fun `unknown and missing keys fall back to the default style`() {
        assertEquals(NoteStyleRef.DEFAULT, NoteStyleRef.fromStorageKey(null))
        assertEquals(NoteStyleRef.DEFAULT, NoteStyleRef.fromStorageKey("nonsense"))
        assertEquals(NoteStyleRef.DEFAULT, NoteStyleRef.fromStorageKey("custom:abc"))
    }
}
