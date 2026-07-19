package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.AiResult
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.TokenEstimator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptStructurerTest {

    private val aiBackend: AiBackend = mockk()
    private val structurer = TranscriptStructurer(aiBackend)

    private fun result(text: String, timeMs: Long = 100) = AiResult(
        text = text,
        processingTimeMs = timeMs,
        inputTokens = 10,
        outputTokens = 10,
        hardwareBackend = HardwareBackend.CPU,
    )

    @Test
    fun `single call produces title and overview, transcript stays verbatim`() = runTest {
        coEvery { aiBackend.processText(RecordingPrompts.STRUCTURE_NOTE_JSON, any()) } returns
            result(
                "<thinking>Two segments about a weekly sync.</thinking>\n" +
                    """{"title": "Weekly sync notes", "overview": "## Decisions\n- Ship Friday"}""",
                timeMs = 250,
            )

        val note = structurer.structure(listOf("uh so like weekly sync", "um we ship friday"))

        assertEquals("Weekly sync notes", note.title)
        assertEquals("## Decisions\n- Ship Friday", note.overview)
        assertEquals("uh so like weekly sync\n\num we ship friday", note.transcript)
        assertEquals(250, note.structuringTimeMs)
        coVerify(exactly = 1) {
            aiBackend.processText(
                RecordingPrompts.STRUCTURE_NOTE_JSON,
                "\"uh so like weekly sync\",\n\n\"um we ship friday\"",
            )
        }
    }

    @Test
    fun `double quotes inside a chunk are replaced before quoting`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("""{"title": "Quotes", "overview": "- noted"}""")

        structurer.structure(listOf("""he said "ship it" today"""))

        coVerify {
            aiBackend.processText(
                RecordingPrompts.STRUCTURE_NOTE_JSON,
                "\"he said 'ship it' today\"",
            )
        }
    }

    @Test
    fun `code-fenced json output is still parsed`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("```json\n{\"title\": \"Fenced\", \"overview\": \"- body\"}\n```")

        val note = structurer.structure(listOf("short transcript"))

        assertEquals("Fenced", note.title)
        assertEquals("- body", note.overview)
    }

    @Test
    fun `real line breaks inside json values still parse`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("{\"title\": \"Планування релізу\", \"overview\": \"## Рішення\n- Реліз у п'ятницю\n- Тестуємо у четвер\"}")

        val note = structurer.structure(listOf("коротка розмова"))

        assertEquals("Планування релізу", note.title)
        assertEquals("## Рішення\n- Реліз у п'ятницю\n- Тестуємо у четвер", note.overview)
    }

    @Test
    fun `broken json falls back to regex field extraction without leaking scaffolding`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("{\"title\": \"Team sync\", \"overview\": \"- point one\\n- point two\", }")

        val note = structurer.structure(listOf("short"))

        assertEquals("Team sync", note.title)
        assertEquals("- point one\n- point two", note.overview)
        assertTrue(!note.title.contains("{") && !note.title.contains("\"title\""))
    }

    @Test
    fun `unparseable json-like output never leaks braces or field names`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("```json\n{\"headline\": broken, no fields here}\n```")

        val note = structurer.structure(listOf("short"))

        assertTrue(!note.overview.contains("{") && !note.overview.contains("```"))
        assertTrue(!note.title.contains("{") && !note.title.contains("json"))
    }

    @Test
    fun `malformed output falls back to raw text overview and first-words title`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("Budget approved for the next quarter of work")

        val note = structurer.structure(listOf("short transcript"))

        assertEquals("Budget approved for the next quarter of work", note.overview)
        assertEquals("Budget approved for the next quarter of work", note.title)
    }

    @Test
    fun `blank model output falls back to the transcript`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns result("   ")

        val note = structurer.structure(listOf("part one"))

        assertEquals("part one", note.overview)
        assertEquals("part one", note.title)
    }

    @Test
    fun `over-budget transcript is structured in segments with one call each`() = runTest {
        val paragraph = "word ".repeat(2_000).trim()
        val longChunks = List(8) { paragraph }
        coEvery { aiBackend.processText(RecordingPrompts.STRUCTURE_NOTE_JSON, any()) } returns
            result("""{"title": "Long meeting recap", "overview": "## Section\n- point"}""")

        val note = structurer.structure(longChunks)

        assertEquals("Long meeting recap", note.title)
        assertTrue(note.transcript.startsWith(paragraph))
        val segmentCount = structurer.splitIntoSegments(
            longChunks.joinToString(",\n\n") { "\"$it\"" },
        ).size
        assertTrue(segmentCount > 1)
        coVerify(exactly = segmentCount) {
            aiBackend.processText(RecordingPrompts.STRUCTURE_NOTE_JSON, any())
        }
        assertEquals(
            List(segmentCount) { "## Section\n- point" }.joinToString("\n\n"),
            note.overview,
        )
    }

    @Test
    fun `segments respect token budget`() {
        val paragraph = "word ".repeat(2_000).trim()
        val longTranscript = List(8) { paragraph }.joinToString("\n\n")

        val segments = structurer.splitIntoSegments(longTranscript)

        assertTrue(segments.size > 1)
        segments.forEach { segment ->
            assertTrue(TokenEstimator.approxText(segment) <= 2_500)
        }
    }

    @Test
    fun `cyrillic transcript splits into denser segments`() {
        val paragraph = "слово ".repeat(1_000).trim()
        val longTranscript = List(8) { paragraph }.joinToString("\n\n")

        val segments = structurer.splitIntoSegments(longTranscript)

        assertTrue(segments.size > 1)
        segments.forEach { segment ->
            assertTrue(TokenEstimator.approxText(segment) <= 2_500)
        }
    }

    @Test
    fun `short transcript is a single segment`() {
        assertEquals(1, structurer.splitIntoSegments("short one").size)
    }
}
