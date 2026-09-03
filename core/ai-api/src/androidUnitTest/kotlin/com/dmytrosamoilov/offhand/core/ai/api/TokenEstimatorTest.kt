package com.dmytrosamoilov.offhand.core.ai.api

import org.junit.Assert.assertEquals
import org.junit.Test

class TokenEstimatorTest {

    @Test
    fun `ascii text estimate rounds up at four chars per token`() {
        assertEquals(0, TokenEstimator.approxText(""))
        assertEquals(1, TokenEstimator.approxText("a"))
        assertEquals(1, TokenEstimator.approxText("abcd"))
        assertEquals(2, TokenEstimator.approxText("abcde"))
        assertEquals(25, TokenEstimator.approxText("x".repeat(100)))
    }

    @Test
    fun `cyrillic text estimate uses two chars per token`() {
        assertEquals(50, TokenEstimator.approxText("б".repeat(100)))
    }

    @Test
    fun `mixed text sums per-script estimates`() {
        assertEquals(75, TokenEstimator.approxText("x".repeat(100) + "б".repeat(100)))
    }
}
