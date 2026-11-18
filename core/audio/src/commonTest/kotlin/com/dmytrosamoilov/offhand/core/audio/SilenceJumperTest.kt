package com.dmytrosamoilov.offhand.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SilenceJumperTest {

    private val skips = listOf(SilenceSkip(startMs = 1_250, endMs = 5_000), SilenceSkip(startMs = 7_000, endMs = 9_500))

    @Test
    fun `speech is played without a jump`() {
        val jumper = SilenceJumper(skips)

        assertNull(jumper.jumpTargetAt(600))
        assertNull(jumper.jumpTargetAt(5_000))
    }

    @Test
    fun `a pause is jumped to its end`() {
        val jumper = SilenceJumper(skips)

        assertEquals(5_000, jumper.jumpTargetAt(1_260))
    }

    @Test
    fun `a position still read from inside the pause does not jump again`() {
        val jumper = SilenceJumper(skips)
        jumper.jumpTargetAt(1_260)

        val repeats = listOf(4_800L, 4_840L, 4_880L, 4_990L).mapNotNull { jumper.jumpTargetAt(it) }

        assertTrue(repeats.isEmpty())
    }

    @Test
    fun `a position read from before the pause does not jump again`() {
        val jumper = SilenceJumper(skips)
        jumper.jumpTargetAt(1_260)

        assertNull(jumper.jumpTargetAt(1_200))
        assertNull(jumper.jumpTargetAt(1_300))
    }

    @Test
    fun `every pause of a recording is jumped once`() {
        val jumper = SilenceJumper(skips)
        val readings = listOf(1_240L, 1_280L, 4_850L, 4_890L, 5_010L, 6_990L, 7_030L, 9_300L, 9_340L, 9_520L, 9_560L)

        val jumps = readings.mapNotNull { jumper.jumpTargetAt(it) }

        assertEquals(listOf(5_000L, 9_500L), jumps)
    }

    @Test
    fun `seeking back into a pause jumps it again`() {
        val jumper = SilenceJumper(skips)
        jumper.jumpTargetAt(1_260)
        jumper.jumpTargetAt(5_040)

        jumper.onSeek(2_000)

        assertEquals(5_000, jumper.jumpTargetAt(2_000))
    }

    @Test
    fun `a stale position after a seek is not taken for a pause`() {
        val jumper = SilenceJumper(skips)

        jumper.onSeek(5_100)

        assertNull(jumper.jumpTargetAt(4_900))
        assertNull(jumper.jumpTargetAt(5_140))
    }

    @Test
    fun `the position shown never falls back behind a jump`() {
        val jumper = SilenceJumper(skips)
        jumper.jumpTargetAt(1_260)

        assertEquals(5_000, jumper.shownPosition(4_800))
        assertEquals(5_200, jumper.shownPosition(5_200))
    }

    @Test
    fun `no pauses means nothing to do`() {
        val jumper = SilenceJumper(emptyList())

        assertTrue(jumper.isEmpty)
        assertNull(jumper.jumpTargetAt(3_000))
    }
}
