package com.dmytrosamoilov.offhand.feature.notes.domain.export

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownBlockParserTest {

    @Test
    fun `headings bullets numbers and paragraphs become blocks`() {
        val markdown = """
            ## Main Topics
            - **Budget** approved
            * Hiring plan
            1. Send the report
            2) Call Anna

            A closing sentence
            that wraps.
        """.trimIndent()

        val blocks = MarkdownBlockParser.parse(markdown)

        assertEquals(
            listOf(
                DocumentBlock.Heading("Main Topics"),
                DocumentBlock.Bullet("Budget approved"),
                DocumentBlock.Bullet("Hiring plan"),
                DocumentBlock.Numbered(1, "Send the report"),
                DocumentBlock.Numbered(2, "Call Anna"),
                DocumentBlock.Paragraph("A closing sentence that wraps."),
            ),
            blocks,
        )
    }

    @Test
    fun `inline markers are stripped but asterisks inside words survive`() {
        assertEquals("bold and italic and code", MarkdownBlockParser.inline("**bold** and *italic* and `code`"))
        assertEquals("2*3 equals 6", MarkdownBlockParser.inline("2*3 equals 6"))
        assertEquals("snake_case name", MarkdownBlockParser.inline("snake_case name"))
    }
}
