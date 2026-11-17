package com.dmytrosamoilov.offhand.feature.notes.presentation

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NoteDetailExtrasTest {

    private val parts = listOf(
        TranscriptPartUi(startMs = 0, time = "0:00", text = "first"),
        TranscriptPartUi(startMs = 29_000, time = "0:29", text = "second"),
        TranscriptPartUi(startMs = 58_000, time = "0:58", text = "third"),
    )

    private val note = NoteDetailUi(
        id = 1,
        title = "Note",
        body = "Summary",
        transcript = "first\n\nsecond\n\nthird",
        createdAt = "",
        wordCount = 0,
        hasAudio = true,
        metrics = null,
        status = NoteStatusUi.READY,
        style = NoteStyleRef.BuiltIn(NotePreset.SUMMARY),
        transcriptParts = parts,
    )

    private fun playing(positionMs: Long, isPlaying: Boolean = true) =
        NotesUiState(selected = note, playback = AudioPlaybackUi(isAvailable = true, isPlaying = isPlaying, positionMs = positionMs))

    @Test
    fun `the paragraph being played is the last one that started before the position`() {
        assertEquals(0, playing(positionMs = 1_000).playingPartIndex)
        assertEquals(1, playing(positionMs = 29_000).playingPartIndex)
        assertEquals(2, playing(positionMs = 70_000).playingPartIndex)
    }

    @Test
    fun `no paragraph is marked while paused or without time marks`() {
        assertNull(playing(positionMs = 30_000, isPlaying = false).playingPartIndex)
        assertNull(playing(positionMs = 30_000).copy(selected = note.copy(transcriptParts = emptyList())).playingPartIndex)
    }

    @Test
    fun `moving into a folder with its own style rewrites the notes only for Pro on a phone that writes summaries`() {
        val folders = listOf(
            FolderUi(id = 1, name = "Work", noteCount = 0, style = NoteStyleRef.BuiltIn(NotePreset.MEETING)),
            FolderUi(id = 2, name = "Ideas", noteCount = 0),
        )
        val pro = NotesUiState(folders = folders, isFolderStylesUnlocked = true)

        assertTrue(pro.rewritesNotesMovedTo(1))
        assertFalse(pro.rewritesNotesMovedTo(2))
        assertFalse(pro.rewritesNotesMovedTo(null))
        assertFalse(pro.copy(isFolderStylesUnlocked = false).rewritesNotesMovedTo(1))
        assertFalse(pro.copy(isSummaryAvailable = false).rewritesNotesMovedTo(1))
    }
}
