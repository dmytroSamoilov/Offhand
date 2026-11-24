package com.dmytrosamoilov.offhand.feature.notes.presentation

import com.dmytrosamoilov.offhand.core.data.domain.Note
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranscriptPartsTest {

    private fun note(transcript: String, startsMs: List<Long>) = Note(
        id = 1,
        title = "Note",
        body = "",
        transcript = transcript,
        createdAtEpochMs = 0,
        transcriptionTimeMs = null,
        structuringTimeMs = null,
        hardwareBackend = null,
        transcriptStartsMs = startsMs,
    )

    @Test
    fun `each paragraph carries the time it starts at`() {
        val parts = note("first\n\nsecond\n\nthird", listOf(0L, 29_000L, 3_725_000L)).toTranscriptPartsUi()

        assertEquals(listOf("0:00", "0:29", "1:02:05"), parts.map { it.time })
        assertEquals(listOf("first", "second", "third"), parts.map { it.text })
        assertEquals(listOf(0L, 29_000L, 3_725_000L), parts.map { it.startMs })
    }

    @Test
    fun `a transcript whose paragraphs no longer match its time marks shows none`() {
        assertTrue(note("first\n\nsecond\n\nadded by hand", listOf(0L, 29_000L)).toTranscriptPartsUi().isEmpty())
        assertTrue(note("an older note", emptyList()).toTranscriptPartsUi().isEmpty())
        assertTrue(note("", emptyList()).toTranscriptPartsUi().isEmpty())
    }
}
