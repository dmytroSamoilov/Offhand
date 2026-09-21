package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLimits
import com.dmytrosamoilov.offhand.core.data.domain.SectionFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NoteStyleDraftParserTest {

    @Test
    fun `thinking block is dropped and the json becomes a draft`() {
        val raw = "<thinking>A debrief needs the customer.</thinking>\n" +
            """{"name": "Sales debrief", "kind": "a sales call debrief.", "sections": [""" +
            """{"heading": "## Customer", "guidance": "who the customer is", "format": "sentences"}, """ +
            """{"heading": "Next steps", "guidance": "each task with who and when.", "format": "Bullets"}, """ +
            """{"heading": "Notes", "guidance": "", "format": "free"}]}"""

        val draft = assertNotNull(NoteStyleDraftParser.parse(raw))

        assertEquals("Sales debrief", draft.name)
        assertEquals("a sales call debrief", draft.noteKind)
        assertEquals(listOf("Customer", "Next steps", "Notes"), draft.sections.map { it.heading })
        assertEquals(listOf(SectionFormat.SENTENCES, SectionFormat.BULLETS, SectionFormat.FREE), draft.sections.map { it.format })
        assertEquals("each task with who and when", draft.sections[1].guidance)
    }

    @Test
    fun `unknown format falls back to sentences and oversized fields are cut to the limits`() {
        val raw = """{"name": "${"n".repeat(80)}", "sections": [{"heading": "${"h".repeat(80)}", "format": "paragraph"}]}"""

        val draft = assertNotNull(NoteStyleDraftParser.parse(raw))

        assertEquals(NoteStyleLimits.MAX_NAME_LENGTH, draft.name.length)
        assertEquals(NoteStyleLimits.MAX_HEADING_LENGTH, draft.sections.single().heading.length)
        assertEquals(SectionFormat.SENTENCES, draft.sections.single().format)
    }

    @Test
    fun `extra sections are dropped and blank headings skipped`() {
        val sections = List(NoteStyleLimits.MAX_SECTIONS + 2) { """{"heading": "H$it", "format": "bullets"}""" }
        val raw = """{"name": "x", "sections": [{"heading": "  "}, ${sections.joinToString()}]}"""

        val draft = assertNotNull(NoteStyleDraftParser.parse(raw))

        assertEquals(NoteStyleLimits.MAX_SECTIONS, draft.sections.size)
        assertEquals("H0", draft.sections.first().heading)
    }

    @Test
    fun `responses without usable sections are rejected`() {
        assertNull(NoteStyleDraftParser.parse("I cannot help with that."))
        assertNull(NoteStyleDraftParser.parse("""{"name": "x", "sections": []}"""))
        assertNull(NoteStyleDraftParser.parse("<thinking>still thinking"))
    }
}
