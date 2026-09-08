package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.feature.notes.domain.TextMatch
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class SearchNotesUseCaseTest {

    private val searchNotes = SearchNotesUseCase()

    private val budgetNote = note(
        id = 1,
        title = "Budget review",
        body = "## Summary\nWe agreed to **cut** the marketing budget by ten percent.",
        transcript = "So the marketing budget goes down by ten percent next quarter.",
    )
    private val hiringNote = note(
        id = 2,
        title = "Hiring plan",
        body = "Two backend engineers in Q3.",
        transcript = "We want two backend engineers by September.",
    )

    @Test
    fun `blank query returns every note without highlights`() {
        val results = searchNotes(listOf(budgetNote, hiringNote), "   ")

        assertEquals(listOf(budgetNote, hiringNote), results.map { it.note })
        assertTrue(results.all { it.titleMatches.isEmpty() && it.snippetMatches.isEmpty() })
        assertNull(results.first().snippet)
    }

    @Test
    fun `notes without a match are dropped`() {
        val results = searchNotes(listOf(budgetNote, hiringNote), "marketing")

        assertEquals(listOf(budgetNote), results.map { it.note })
    }

    @Test
    fun `title matches are case insensitive and highlighted`() {
        val result = searchNotes(listOf(budgetNote), "BUDGET").single()

        assertEquals(listOf(TextMatch(start = 0, end = 6)), result.titleMatches)
    }

    @Test
    fun `snippet comes from the cleaned body with highlights`() {
        val result = searchNotes(listOf(budgetNote), "cut").single()

        assertEquals("Summary We agreed to cut the marketing budget by ten percent.", result.snippet)
        assertEquals(listOf(TextMatch(start = 21, end = 24)), result.snippetMatches)
    }

    @Test
    fun `snippet falls back to the transcript when only it matches`() {
        val result = searchNotes(listOf(budgetNote), "quarter").single()

        assertEquals("So the marketing budget goes down by ten percent next quarter.", result.snippet)
        assertEquals(listOf(TextMatch(start = 54, end = 61)), result.snippetMatches)
    }

    @Test
    fun `every term must match somewhere in the note`() {
        val results = searchNotes(listOf(budgetNote, hiringNote), "budget september")

        assertTrue(results.isEmpty())
    }

    @Test
    fun `terms may match across different fields`() {
        val result = searchNotes(listOf(hiringNote), "hiring september").single()

        assertEquals(listOf(TextMatch(start = 0, end = 6)), result.titleMatches)
        assertEquals("We want two backend engineers by September.", result.snippet)
    }

    @Test
    fun `long body is windowed around the first match with an ellipsis`() {
        val filler = (1..40).joinToString(" ") { "word$it" }
        val note = note(id = 3, title = "Long", body = "$filler needle appears late", transcript = "")

        val result = searchNotes(listOf(note), "needle").single()

        val snippet = requireNotNull(result.snippet)
        assertTrue(snippet.startsWith("… "))
        val match = result.snippetMatches.single()
        assertEquals("needle", snippet.substring(match.start, match.end))
    }

    @Test
    fun `overlapping term matches are merged`() {
        val note = note(id = 4, title = "Notebook notes", body = "", transcript = "")

        val result = searchNotes(listOf(note), "note notebook").single()

        assertEquals(listOf(TextMatch(start = 0, end = 8), TextMatch(start = 9, end = 13)), result.titleMatches)
    }

    private fun note(id: Long, title: String, body: String, transcript: String) = Note(
        id = id,
        title = title,
        body = body,
        transcript = transcript,
        createdAtEpochMs = 0,
        transcriptionTimeMs = null,
        structuringTimeMs = null,
        hardwareBackend = null,
    )
}
