package com.dmytrosamoilov.offhand.feature.notes.presentation

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StyleChipsTest {

    private val customStyles = listOf(
        NoteStyleOptionUi(id = 7, name = "Client update", description = ""),
        NoteStyleOptionUi(id = 9, name = "In my voice", description = ""),
    )

    private fun note(
        style: NoteStyleRef,
        status: NoteStatusUi = NoteStatusUi.READY,
        transcript: String = "Team meeting about the budget.",
    ) = NoteDetailUi(
        id = 1,
        title = "Note",
        body = "Summary",
        transcript = transcript,
        createdAt = "",
        wordCount = 0,
        hasAudio = true,
        metrics = null,
        status = status,
        style = style,
    )

    private fun chips(note: NoteDetailUi) = NotesUiState(selected = note, customStyles = customStyles).styleChips

    @Test
    fun `the current style comes first then the own styles then the other built-in ones`() {
        val result = chips(note(NoteStyleRef.BuiltIn(NotePreset.MEETING)))

        assertEquals(
            listOf(
                NoteStyleRef.BuiltIn(NotePreset.MEETING),
                NoteStyleRef.Custom(7),
                NoteStyleRef.Custom(9),
                NoteStyleRef.BuiltIn(NotePreset.SUMMARY),
                NoteStyleRef.BuiltIn(NotePreset.VISIT),
                NoteStyleRef.BuiltIn(NotePreset.LEGAL),
                NoteStyleRef.BuiltIn(NotePreset.EMAIL),
            ),
            result.map { it.style },
        )
        assertEquals(listOf(true, false, false, false, false, false, false), result.map { it.isCurrent })
    }

    @Test
    fun `a note in one of the own styles leads with that style and its name`() {
        val result = chips(note(NoteStyleRef.Custom(9)))

        assertEquals(StyleChipUi(NoteStyleRef.Custom(9), "In my voice", isCurrent = true), result.first())
        assertEquals(NoteStyleRef.Custom(7), result[1].style)
        assertEquals(customStyles.size + NotePreset.entries.size, result.size)
    }

    @Test
    fun `no chips while the note is not finished or has no transcript`() {
        val summary = NoteStyleRef.BuiltIn(NotePreset.SUMMARY)

        assertTrue(chips(note(summary, status = NoteStatusUi.PROCESSING)).isEmpty())
        assertTrue(chips(note(summary, status = NoteStatusUi.INTERRUPTED)).isEmpty())
        assertTrue(chips(note(summary, status = NoteStatusUi.FAILED)).isEmpty())
        assertTrue(chips(note(summary, transcript = " ")).isEmpty())
        assertTrue(NotesUiState(customStyles = customStyles).styleChips.isEmpty())
    }
}
