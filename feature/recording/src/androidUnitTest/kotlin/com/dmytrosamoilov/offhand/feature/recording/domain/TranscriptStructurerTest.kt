package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.AiBackendException
import com.dmytrosamoilov.offhand.core.ai.api.AiResult
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.TokenEstimator
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptStructurerTest {

    private val aiBackend: AiBackend = mockk()
    private val structurer = TranscriptStructurer(aiBackend, testModelManager())

    private fun result(text: String, timeMs: Long = 100) = AiResult(
        text = text,
        processingTimeMs = timeMs,
        inputTokens = 10,
        outputTokens = 10,
        hardwareBackend = HardwareBackend.CPU,
    )

    private fun stubPolish(preset: NotePreset, json: String, timeMs: Long = 100) {
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.polishNote(preset), any()) } returns
            result(json, timeMs = timeMs)
    }

    @Test
    fun `single call produces title and overview, transcript stays verbatim`() = runTest {
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(NotePreset.MEETING), any()) } returns
            result(
                "<thinking>Two segments about a weekly sync.</thinking>\n" +
                    """{"title": "Weekly sync notes", "overview": "## Decisions\n- Ship Friday"}""",
                timeMs = 250,
            )
        stubPolish(
            NotePreset.MEETING,
            """{"title": "Weekly sync notes", "overview": "## Decisions\n- Ship Friday"}""",
        )

        val note = structurer.structure(
            preset = NotePreset.MEETING,
            chunkTranscripts = listOf("uh so like weekly sync", "um we ship friday"),
        )

        assertEquals("Weekly sync notes", note.title)
        assertEquals("## Decisions\n- Ship Friday", note.overview)
        assertEquals("uh so like weekly sync\n\num we ship friday", note.transcript)
        assertEquals(350, note.structuringTimeMs)
        coVerify(exactly = 1) {
            aiBackend.processText(
                ModelPromptSet.Gemma4.structureNote(NotePreset.MEETING),
                "\"uh so like weekly sync\",\n\n\"um we ship friday\"",
            )
        }
    }

    @Test
    fun `final polish pass rewrites the merged overview and the title`() = runTest {
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(NotePreset.MEETING), any()) } returns
            result(
                """{"title": "Sync", "overview": "## Decisions\n- Ship on Friday\n- We ship Friday"}""",
                timeMs = 200,
            )
        stubPolish(
            NotePreset.MEETING,
            """{"title": "Weekly sync", "overview": "## Decisions\n- Ship on Friday"}""",
            timeMs = 150,
        )

        val note = structurer.structure(listOf("we ship friday"), NotePreset.MEETING)

        assertEquals("Weekly sync", note.title)
        assertEquals("## Decisions\n- Ship on Friday", note.overview)
        assertEquals(350, note.structuringTimeMs)
        coVerify(exactly = 1) {
            aiBackend.processText(
                ModelPromptSet.Gemma4.polishNote(NotePreset.MEETING),
                "## Decisions\n- Ship on Friday\n- We ship Friday",
            )
        }
    }

    @Test
    fun `polish output that loses most of the note is discarded`() = runTest {
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(NotePreset.SUMMARY), any()) } returns
            result(
                """{"title": "Budget review", "overview": "I reviewed the quarterly budget with the finance team and we agreed to move four thousand into marketing for October."}""",
            )
        stubPolish(NotePreset.SUMMARY, """{"title": "Budget", "overview": "Reviewed."}""")

        val note = structurer.structure(listOf("budget talk"), NotePreset.SUMMARY)

        assertEquals("Budget review", note.title)
        assertTrue(note.overview.startsWith("I reviewed the quarterly budget"))
    }

    @Test
    fun `polish backend failure keeps the merged overview`() = runTest {
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(NotePreset.SUMMARY), any()) } returns
            result("""{"title": "My day", "overview": "I shipped the build."}""", timeMs = 200)
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.polishNote(NotePreset.SUMMARY), any()) } throws
            AiBackendException("engine busy")

        val note = structurer.structure(listOf("shipping talk"), NotePreset.SUMMARY)

        assertEquals("My day", note.title)
        assertEquals("I shipped the build.", note.overview)
        assertEquals(200, note.structuringTimeMs)
    }

    @Test
    fun `overview past the polish budget skips the polish pass`() = runTest {
        val longOverview = "word ".repeat(3_000).trim()
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(NotePreset.SUMMARY), any()) } returns
            result("""{"title": "Long", "overview": "$longOverview"}""")

        val note = structurer.structure(listOf("short"), NotePreset.SUMMARY)

        assertEquals(longOverview, note.overview)
        coVerify(exactly = 0) {
            aiBackend.processText(ModelPromptSet.Gemma4.polishNote(NotePreset.SUMMARY), any())
        }
    }

    @Test
    fun `double quotes in the merged overview are replaced before polishing`() = runTest {
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(NotePreset.SUMMARY), any()) } returns
            result("""{"title": "Quotes", "overview": "he said \"ship it\" today"}""")
        stubPolish(NotePreset.SUMMARY, """{"title": "Quotes", "overview": "he said 'ship it' today"}""")

        val note = structurer.structure(listOf("quoting"), NotePreset.SUMMARY)

        assertEquals("he said 'ship it' today", note.overview)
        coVerify(exactly = 1) {
            aiBackend.processText(
                ModelPromptSet.Gemma4.polishNote(NotePreset.SUMMARY),
                "he said 'ship it' today",
            )
        }
    }

    @Test
    fun `double quotes inside a chunk are replaced before quoting`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("""{"title": "Quotes", "overview": "- noted"}""")

        structurer.structure(
            preset = NotePreset.MEETING,
            chunkTranscripts = listOf("""he said "ship it" today"""),
        )

        coVerify {
            aiBackend.processText(
                ModelPromptSet.Gemma4.structureNote(NotePreset.MEETING),
                "\"he said 'ship it' today\"",
            )
        }
    }

    @Test
    fun `code-fenced json output is still parsed`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("```json\n{\"title\": \"Fenced\", \"overview\": \"- body\"}\n```")

        val note = structurer.structure(listOf("short transcript"), NotePreset.MEETING)

        assertEquals("Fenced", note.title)
        assertEquals("- body", note.overview)
    }

    @Test
    fun `real line breaks inside json values still parse`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("{\"title\": \"Планування релізу\", \"overview\": \"## Рішення\n- Реліз у п'ятницю\n- Тестуємо у четвер\"}")

        val note = structurer.structure(listOf("коротка розмова"), NotePreset.MEETING)

        assertEquals("Планування релізу", note.title)
        assertEquals("## Рішення\n- Реліз у п'ятницю\n- Тестуємо у четвер", note.overview)
    }

    @Test
    fun `broken json falls back to regex field extraction without leaking scaffolding`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("{\"title\": \"Team sync\", \"overview\": \"- point one\\n- point two\", }")

        val note = structurer.structure(listOf("short"), NotePreset.MEETING)

        assertEquals("Team sync", note.title)
        assertEquals("- point one\n- point two", note.overview)
        assertTrue(!note.title.contains("{") && !note.title.contains("\"title\""))
    }

    @Test
    fun `overview missing its closing quote is kept instead of falling back to the transcript`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("```json\n{\n\"title\": \"Team sync\",\n\"overview\": \"## Discussion\\n- We shipped on Friday.\\n}\n```")

        val note = structurer.structure(listOf("we shipped on friday"), NotePreset.MEETING)

        assertEquals("Team sync", note.title)
        assertEquals("## Discussion\n- We shipped on Friday.", note.overview)
    }

    @Test
    fun `unterminated overview in a summary note does not become the raw transcript`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("{\"title\": \"My day\", \"overview\": \"I shipped the build today.\\n}")

        val note = structurer.structure(listOf("raw spoken words"), NotePreset.SUMMARY)

        assertEquals("I shipped the build today.", note.overview)
    }

    @Test
    fun `unparseable json-like output never leaks braces or field names`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("```json\n{\"headline\": broken, no fields here}\n```")

        val note = structurer.structure(listOf("short"), NotePreset.SUMMARY)

        assertTrue(!note.overview.contains("{") && !note.overview.contains("```"))
        assertTrue(!note.title.contains("{") && !note.title.contains("json"))
    }

    @Test
    fun `malformed output falls back to raw text overview and first-words title`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("Budget approved for the next quarter of work")

        val note = structurer.structure(listOf("short transcript"), NotePreset.SUMMARY)

        assertEquals("Budget approved for the next quarter of work", note.overview)
        assertEquals("Budget approved for the next quarter of work", note.title)
    }

    @Test
    fun `blank model output falls back to the transcript`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns result("   ")

        val note = structurer.structure(listOf("part one"), NotePreset.SUMMARY)

        assertEquals("part one", note.overview)
        assertEquals("part one", note.title)
    }

    @Test
    fun `over-budget transcript is structured in segments with one call each`() = runTest {
        val paragraph = "word ".repeat(2_000).trim()
        val longChunks = List(8) { paragraph }
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(NotePreset.MEETING), any()) } returns
            result("""{"title": "Long meeting recap", "overview": "## Decisions\n- point"}""")
        stubPolish(NotePreset.MEETING, """{"title": "Long meeting recap", "overview": "## Decisions\n- point"}""")

        val note = structurer.structure(longChunks, NotePreset.MEETING)

        assertEquals("Long meeting recap", note.title)
        assertTrue(note.transcript.startsWith(paragraph))
        val segmentCount = structurer.splitIntoSegments(
            longChunks.joinToString(",\n\n") { "\"$it\"" },
            structurer.segmentTokenBudget(ModelPromptSet.Gemma4.structureNote(NotePreset.MEETING)),
        ).size
        assertTrue(segmentCount > 1)
        coVerify(exactly = segmentCount) {
            aiBackend.processText(ModelPromptSet.Gemma4.structureNote(NotePreset.MEETING), any())
        }
        assertEquals("## Decisions\n- point", note.overview)
    }

    @Test
    fun `summary preset strips headings and bullets the model still emits`() = runTest {
        coEvery { aiBackend.processText(any(), any()) } returns
            result("""{"title": "My day", "overview": "## Today\n- I am tired\n- I ship on Friday"}""")

        val note = structurer.structure(listOf("short transcript"), NotePreset.SUMMARY)

        assertEquals("Today\nI am tired\nI ship on Friday", note.overview)
    }

    @Test
    fun `segments respect token budget`() {
        val paragraph = "word ".repeat(2_000).trim()
        val longTranscript = List(8) { paragraph }.joinToString("\n\n")

        val segments = structurer.splitIntoSegments(longTranscript, 2_500)

        assertTrue(segments.size > 1)
        segments.forEach { segment ->
            assertTrue(TokenEstimator.approxText(segment) <= 2_500)
        }
    }

    @Test
    fun `cyrillic transcript splits into denser segments`() {
        val paragraph = "слово ".repeat(1_000).trim()
        val longTranscript = List(8) { paragraph }.joinToString("\n\n")

        val segments = structurer.splitIntoSegments(longTranscript, 2_500)

        assertTrue(segments.size > 1)
        segments.forEach { segment ->
            assertTrue(TokenEstimator.approxText(segment) <= 2_500)
        }
    }

    @Test
    fun `short transcript is a single segment`() {
        assertEquals(1, structurer.splitIntoSegments("short one", 2_500).size)
    }

    @Test
    fun `transcript-only fallback keeps the verbatim text and derives a title`() {
        val note = structurer.transcriptOnly(
            listOf("budget approved for next quarter", "second thought"),
        )

        assertEquals("budget approved for next quarter\n\nsecond thought", note.transcript)
        assertEquals(note.transcript, note.overview)
        assertEquals("budget approved for next quarter", note.title)
        assertEquals(0, note.structuringTimeMs)
    }

    @Test
    fun `segment budget leaves prompt and output headroom in every preset`() {
        NotePreset.entries.forEach { preset ->
            val prompt = ModelPromptSet.Gemma4.structureNote(preset)
            val budget = structurer.segmentTokenBudget(prompt)

            assertTrue("$preset budget must stay positive", budget > 0)
            assertTrue(
                "$preset budget must leave at least 1000 output tokens",
                budget + TokenEstimator.approxText(prompt) + 1_000 <= testModel().maxTokens,
            )
        }
    }

    @Test
    fun `polish budget leaves room for the note twice in every preset`() {
        NotePreset.entries.forEach { preset ->
            val prompt = ModelPromptSet.Gemma4.polishNote(preset)
            val budget = structurer.polishTokenBudget(prompt)

            assertTrue("$preset polish budget must stay positive", budget > 0)
            assertTrue(
                "$preset polish budget must fit input and output in the context window",
                budget * 2 + TokenEstimator.approxText(prompt) <= testModel().maxTokens,
            )
        }
    }
}
