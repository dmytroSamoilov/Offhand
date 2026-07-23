package com.dmytrosamoilov.offhand.feature.notes.domain

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteShareFormatterTest {

    private val zone = ZoneOffset.UTC
    private val createdAtEpochMs = 1_750_000_000_000
    private val labels = NoteShareLabels(
        title = "Title",
        date = "Date",
        overview = "Overview",
        transcript = "Transcript",
    )

    @Test
    fun `file base name contains title and timestamp`() {
        val name = NoteShareFormatter.fileBaseName(
            title = "Board meeting",
            fallbackTitle = "Recording",
            createdAtEpochMs = createdAtEpochMs,
            zone = zone,
        )

        assertEquals("Board meeting 2025-06-15 15-06", name)
    }

    @Test
    fun `file base name strips illegal filename characters`() {
        val name = NoteShareFormatter.fileBaseName(
            title = "Q3: plan / review * \"draft\"?",
            fallbackTitle = "Recording",
            createdAtEpochMs = createdAtEpochMs,
            zone = zone,
        )

        assertEquals("Q3 plan review draft 2025-06-15 15-06", name)
    }

    @Test
    fun `file base name falls back when title has no valid characters`() {
        val name = NoteShareFormatter.fileBaseName(
            title = "///:::",
            fallbackTitle = "Recording",
            createdAtEpochMs = createdAtEpochMs,
            zone = zone,
        )

        assertEquals("Recording 2025-06-15 15-06", name)
    }

    @Test
    fun `file base name truncates long titles`() {
        val name = NoteShareFormatter.fileBaseName(
            title = "a".repeat(200),
            fallbackTitle = "Recording",
            createdAtEpochMs = createdAtEpochMs,
            zone = zone,
        )

        assertEquals("${"a".repeat(60)} 2025-06-15 15-06", name)
        assertFalse(name.length > 78)
    }

    @Test
    fun `text content lists all fields in order`() {
        val content = NoteShareFormatter.textContent(
            labels = labels,
            title = "Board meeting",
            createdAtEpochMs = createdAtEpochMs,
            overview = "Budget approved.",
            transcript = "We approved the budget.",
            zone = zone,
        )

        val lines = content.lines()
        assertEquals("Title: Board meeting", lines[0])
        assertEquals("Date: Jun 15, 2025 · 15:06", lines[1])
        assertEquals("", lines[2])
        assertEquals("Overview:", lines[3])
        assertEquals("Budget approved.", lines[4])
        assertEquals("", lines[5])
        assertEquals("Transcript:", lines[6])
        assertEquals("We approved the budget.", lines[7])
    }

    @Test
    fun `text content trims surrounding whitespace in sections`() {
        val content = NoteShareFormatter.textContent(
            labels = labels,
            title = "Note",
            createdAtEpochMs = createdAtEpochMs,
            overview = "\n  Overview body  \n",
            transcript = "  Transcript body  ",
            zone = zone,
        )

        assertTrue(content.contains("Overview:\nOverview body\n"))
        assertTrue(content.contains("Transcript:\nTranscript body\n"))
    }
}
