package com.dmytrosamoilov.offhand.feature.settings.domain

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleSection
import com.dmytrosamoilov.offhand.core.data.domain.SectionFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteStyleValidatorTest {

    private val valid = CustomNoteStyle(
        id = 0,
        name = "  Sales   debrief ",
        noteKind = "a sales call debrief",
        language = NoteStyleLanguage.RECORDING,
        sections = listOf(
            NoteStyleSection("## Customer ", "who they are", SectionFormat.SENTENCES),
            NoteStyleSection("Next steps", "", SectionFormat.BULLETS),
        ),
        createdAtEpochMs = 1,
    )

    @Test
    fun `valid style is normalised`() {
        val result = NoteStyleValidator.validate(valid, existing = emptyList()) as NoteStyleValidation.Valid

        assertEquals("Sales debrief", result.style.name)
        assertEquals(listOf("Customer", "Next steps"), result.style.sections.map { it.heading })
    }

    @Test
    fun `blank name, duplicate name and long fields are reported together`() {
        val other = valid.copy(id = 5, name = "Taken")
        val style = valid.copy(
            name = "taken",
            noteKind = "k".repeat(NoteStyleValidator.MAX_KIND_LENGTH + 1),
            sections = listOf(
                NoteStyleSection("", "g".repeat(5000), SectionFormat.BULLETS),
                NoteStyleSection("h".repeat(NoteStyleValidator.MAX_HEADING_LENGTH + 1), "", SectionFormat.BULLETS),
            ),
        )

        val errors = (NoteStyleValidator.validate(style, listOf(other)) as NoteStyleValidation.Invalid).errors

        assertEquals(NoteStyleFieldError.DUPLICATE, errors.name)
        assertEquals(NoteStyleFieldError.TOO_LONG, errors.noteKind)
        assertEquals(NoteStyleFieldError.BLANK, errors.headings[0])
        assertEquals(NoteStyleFieldError.TOO_LONG, errors.headings[1])
    }

    @Test
    fun `editing a style does not collide with its own name`() {
        val stored = valid.copy(id = 5, name = "Sales debrief")

        assertTrue(NoteStyleValidator.validate(stored, listOf(stored)) is NoteStyleValidation.Valid)
    }

    @Test
    fun `duplicate headings are flagged case-insensitively`() {
        val style = valid.copy(
            sections = listOf(
                NoteStyleSection("Facts", "", SectionFormat.BULLETS),
                NoteStyleSection("facts", "", SectionFormat.BULLETS),
            ),
        )

        val errors = (NoteStyleValidator.validate(style, emptyList()) as NoteStyleValidation.Invalid).errors

        assertEquals(setOf(0, 1), errors.headings.keys)
        assertTrue(errors.headings.values.all { it == NoteStyleFieldError.DUPLICATE })
    }

    @Test
    fun `a style without sections is valid and the count is bounded above`() {
        val none = valid.copy(sections = emptyList())
        val tooMany = valid.copy(
            sections = List(NoteStyleValidator.MAX_SECTIONS + 1) { NoteStyleSection("H$it", "", SectionFormat.BULLETS) },
        )

        assertTrue(NoteStyleValidator.validate(none, emptyList()) is NoteStyleValidation.Valid)
        assertEquals(
            NoteStyleSectionsError.TOO_MANY,
            (NoteStyleValidator.validate(tooMany, emptyList()) as NoteStyleValidation.Invalid).errors.sections,
        )
    }
}
