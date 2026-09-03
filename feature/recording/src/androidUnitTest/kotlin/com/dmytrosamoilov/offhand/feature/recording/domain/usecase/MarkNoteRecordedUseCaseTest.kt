package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkNoteRecordedUseCaseTest {

    private val notesRepository: NotesRepository = mockk {
        coJustRun { updateNote(any()) }
    }
    private val useCase = MarkNoteRecordedUseCase(notesRepository)

    private fun recordingNote(id: Long) = Note(
        id = id,
        title = "Recording $id",
        body = "",
        transcript = "",
        createdAtEpochMs = 0,
        transcriptionTimeMs = null,
        structuringTimeMs = null,
        hardwareBackend = null,
        audioFileName = "stale.pcm.enc",
        status = NoteStatus.RECORDING,
    )

    @Test
    fun `stamps duration and audio file and moves the note into processing`() = runTest {
        coEvery { notesRepository.getNote(5L) } returns recordingNote(5L)

        val recorded = useCase(noteId = 5L, durationMs = 90_000L, audioFileName = "final.pcm.enc")

        assertEquals(NoteStatus.PROCESSING, recorded?.status)
        assertEquals(90_000L, recorded?.durationMs)
        assertEquals("final.pcm.enc", recorded?.audioFileName)
        coVerify { notesRepository.updateNote(recorded!!) }
    }

    @Test
    fun `returns null when the note was deleted meanwhile`() = runTest {
        coEvery { notesRepository.getNote(5L) } returns null

        assertNull(useCase(noteId = 5L, durationMs = 90_000L, audioFileName = null))

        coVerify(exactly = 0) { notesRepository.updateNote(any()) }
    }
}
