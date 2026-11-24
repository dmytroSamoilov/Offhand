package com.dmytrosamoilov.offhand.feature.notes.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class NoteWaveformUiTest {

    @Test
    fun `forty sections average into ten bars in fours`() {
        val levels = List(40) { index -> if (index < 4) 100 else 0 }

        val bars = levels.toWaveformUi(LIST_WAVE_BARS)

        assertEquals(10, bars.size)
        assertEquals(1f, bars.first())
        assertEquals(0f, bars[1])
    }

    @Test
    fun `the player keeps every section scaled to one`() {
        assertEquals(listOf(0.5f, 1f, 0f), listOf(50, 100, 0).toWaveformUi())
    }

    @Test
    fun `fewer sections than bars repeat the nearest section`() {
        assertEquals(listOf(0.2f, 0.2f, 0.8f, 0.8f), listOf(20, 80).toWaveformUi(4))
    }

    @Test
    fun `an unmeasured recording has no bars`() {
        assertEquals(emptyList(), emptyList<Int>().toWaveformUi(LIST_WAVE_BARS))
    }
}
