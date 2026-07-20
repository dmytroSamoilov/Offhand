package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.AiBackendException
import com.dmytrosamoilov.offhand.core.ai.api.AiResult
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptProofreaderTest {

    private val aiBackend: AiBackend = mockk()
    private val proofreader = TranscriptProofreader(aiBackend, testModelManager())

    private fun result(text: String, timeMs: Long = 100) = AiResult(
        text = text,
        processingTimeMs = timeMs,
        inputTokens = 10,
        outputTokens = 10,
        hardwareBackend = HardwareBackend.CPU,
    )

    @Test
    fun `punctuation and casing fixes are accepted`() = runTest {
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.proofreadTranscript, any()) } returns
            result("So, um, we need to buy regular milk today for the recipe.")

        val proofread = proofreader.proofread(
            listOf("so um we need to buy regular milk today for the recipe"),
        )

        assertEquals(
            listOf("So, um, we need to buy regular milk today for the recipe."),
            proofread.chunks,
        )
        assertEquals(100, proofread.processingTimeMs)
    }

    @Test
    fun `single word correction within threshold is accepted`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("So um we need to buy regular milk today for the recipe.")

        val proofread = proofreader.proofread(
            listOf("so um we need to buy irregular milk today for the recipe"),
        )

        assertEquals(
            listOf("So um we need to buy regular milk today for the recipe."),
            proofread.chunks,
        )
    }

    @Test
    fun `heavily rewritten output falls back to the raw chunk`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("The speaker plans a shopping trip for dairy products.")

        val proofread = proofreader.proofread(
            listOf("so um we need to buy regular milk today for the recipe"),
        )

        assertEquals(
            listOf("so um we need to buy regular milk today for the recipe"),
            proofread.chunks,
        )
        assertEquals(100, proofread.processingTimeMs)
    }

    @Test
    fun `blank model output keeps the raw chunk`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns result("   ")

        val proofread = proofreader.proofread(listOf("keep this exact text"))

        assertEquals(listOf("keep this exact text"), proofread.chunks)
    }

    @Test
    fun `thinking block is stripped from the cleaned chunk`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("<think>fix punctuation only</think>Keep this exact text.")

        val proofread = proofreader.proofread(listOf("keep this exact text"))

        assertEquals(listOf("Keep this exact text."), proofread.chunks)
    }

    @Test
    fun `backend failure keeps the raw chunk without counting time`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } throws
            AiBackendException("engine unavailable")

        val proofread = proofreader.proofread(listOf("raw chunk survives"))

        assertEquals(listOf("raw chunk survives"), proofread.chunks)
        assertEquals(0, proofread.processingTimeMs)
    }

    @Test
    fun `oversized chunk skips the model entirely`() = runTest {
        val oversized = "word ".repeat(2_000).trim()

        val proofread = proofreader.proofread(listOf(oversized))

        assertEquals(listOf(oversized), proofread.chunks)
        assertEquals(0, proofread.processingTimeMs)
        coVerify(exactly = 0) { aiBackend.processText(any(), any()) }
    }

    @Test
    fun `progress and processing time accumulate across chunks`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returnsMany listOf(
            result("First part.", timeMs = 100),
            result("Second part.", timeMs = 150),
        )
        val fractions = mutableListOf<Float>()

        val proofread = proofreader.proofread(listOf("first part", "second part")) {
            fractions += it
        }

        assertEquals(listOf("First part.", "Second part."), proofread.chunks)
        assertEquals(250, proofread.processingTimeMs)
        assertEquals(listOf(0.5f, 1f), fractions)
    }

    @Test
    fun `faithfulness check rejects reordered and paraphrased text`() {
        assertFalse(proofreader.isFaithful("we ship on friday after the demo", ""))
        assertFalse(
            proofreader.isFaithful(
                "we ship on friday after the demo",
                "after the demo is done the team will ship it",
            ),
        )
        assertTrue(
            proofreader.isFaithful(
                "we ship on friday after the demo and then we rest",
                "We ship on Friday, after the demo — and then we rest.",
            ),
        )
    }
}
