package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SaveNoteTranscriptUseCaseTest {

    private val notesRepository: NotesRepository = mockk {
        coJustRun { updateNote(any()) }
    }
    private val useCase = SaveNoteTranscriptUseCase(notesRepository)

    private fun processingNote(id: Long) = Note(
        id = id,
        title = "Note $id",
        body = "",
        transcript = "",
        createdAtEpochMs = 0,
        transcriptionTimeMs = null,
        structuringTimeMs = null,
        hardwareBackend = null,
        status = NoteStatus.PROCESSING,
    )

    @Test
    fun `saves transcript and transcription time while keeping the processing status`() = runTest {
        coEvery { notesRepository.getNote(5L) } returns processingNote(5L)

        useCase(noteId = 5L, transcript = "spoken words", transcriptionTimeMs = 700)

        coVerify {
            notesRepository.updateNote(
                processingNote(5L).copy(transcript = "spoken words", transcriptionTimeMs = 700),
            )
        }
    }

    @Test
    fun `saves where each paragraph starts and keeps the stored starts when none are given`() = runTest {
        val timed = processingNote(5L).copy(transcriptStartsMs = listOf(0L, 29_000L))
        coEvery { notesRepository.getNote(5L) } returns timed

        useCase(noteId = 5L, transcript = "one\n\ntwo\n\nthree", transcriptionTimeMs = 900, startsMs = listOf(0L, 29_000L, 58_000L))
        useCase(noteId = 5L, transcript = "one\n\ntwo", transcriptionTimeMs = 900)

        coVerify {
            notesRepository.updateNote(
                timed.copy(transcript = "one\n\ntwo\n\nthree", transcriptionTimeMs = 900, transcriptStartsMs = listOf(0L, 29_000L, 58_000L)),
            )
        }
        coVerify { notesRepository.updateNote(timed.copy(transcript = "one\n\ntwo", transcriptionTimeMs = 900)) }
    }

    @Test
    fun `does nothing when the note was deleted meanwhile`() = runTest {
        coEvery { notesRepository.getNote(5L) } returns null

        useCase(noteId = 5L, transcript = "spoken words", transcriptionTimeMs = 700)

        coVerify(exactly = 0) { notesRepository.updateNote(any()) }
    }
}
