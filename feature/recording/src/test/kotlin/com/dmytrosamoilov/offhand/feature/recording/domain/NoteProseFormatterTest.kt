package com.dmytrosamoilov.offhand.feature.recording.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteProseFormatterTest {

    @Test
    fun `bullet and numbered markers are dropped, sentences stay`() {
        val formatted = NoteProseFormatter.format(
            listOf("- I am behind on the release\n* I need Anna to review\n1. I ship on Friday"),
        )

        assertEquals(
            "I am behind on the release\nI need Anna to review\nI ship on Friday",
            formatted,
        )
    }

    @Test
    fun `headings become plain lines`() {
        val formatted = NoteProseFormatter.format(listOf("## My week\nI shipped the parser."))

        assertEquals("My week\nI shipped the parser.", formatted)
    }

    @Test
    fun `paragraph breaks between segments survive`() {
        val formatted = NoteProseFormatter.format(listOf("I started the build.", "I fixed the crash."))

        assertEquals("I started the build.\n\nI fixed the crash.", formatted)
    }

    @Test
    fun `blank line runs collapse to one break`() {
        val formatted = NoteProseFormatter.format(listOf("I woke up late.\n\n\n\nI still shipped."))

        assertEquals("I woke up late.\n\nI still shipped.", formatted)
    }

    @Test
    fun `a line repeated across segments is written once`() {
        val formatted = NoteProseFormatter.format(
            listOf("I am tired today.", "I am tired today.\nI still finished the migration."),
        )

        assertEquals("I am tired today.\n\nI still finished the migration.", formatted)
    }

    @Test
    fun `a dash inside a sentence is left alone`() {
        val formatted = NoteProseFormatter.format(listOf("I shipped it - finally - on Friday."))

        assertEquals("I shipped it - finally - on Friday.", formatted)
    }
}
