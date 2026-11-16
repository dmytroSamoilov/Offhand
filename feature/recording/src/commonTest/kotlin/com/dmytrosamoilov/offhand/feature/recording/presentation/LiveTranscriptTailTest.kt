package com.dmytrosamoilov.offhand.feature.recording.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveTranscriptTailTest {

    @Test
    fun `a short transcript is shown whole`() {
        assertEquals("Team meeting about the budget.", "Team meeting about the budget.".liveTail())
    }

    @Test
    fun `a long transcript shows its end from a word boundary`() {
        val text = (1..60).joinToString(" ") { "word$it" }

        val tail = text.liveTail()

        assertTrue(tail.startsWith("… word"))
        assertTrue(tail.endsWith("word60"))
        assertTrue(tail.length <= 142)
    }
}
