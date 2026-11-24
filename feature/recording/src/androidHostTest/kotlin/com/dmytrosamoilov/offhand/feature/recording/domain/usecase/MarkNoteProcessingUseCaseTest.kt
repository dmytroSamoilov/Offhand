package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NoteSuggestionsRepository
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkNoteProcessingUseCaseTest {

    private val notesRepository: NotesRepository = mockk {
        coJustRun { updateNote(any()) }
    }
    private val noteSuggestionsRepository: NoteSuggestionsRepository = mockk {
        coJustRun { clearSuggestions(any()) }
    }
    private val useCase = MarkNoteProcessingUseCase(notesRepository, noteSuggestionsRepository)

    private val readyNote = Note(
        id = 5,
        title = "Note",
        body = "- done",
        transcript = "done",
        createdAtEpochMs = 0,
        transcriptionTimeMs = 1,
        structuringTimeMs = 1,
        hardwareBackend = "CPU",
        status = NoteStatus.READY,
    )

    @Test
    fun `marks the note processing and drops its smart suggestions`() = runTest {
        coEvery { notesRepository.getNote(5L) } returns readyNote

        val processing = useCase(5L)

        assertEquals(NoteStatus.PROCESSING, processing?.status)
        coVerify { notesRepository.updateNote(readyNote.copy(status = NoteStatus.PROCESSING)) }
        coVerify { noteSuggestionsRepository.clearSuggestions(5L) }
    }

    @Test
    fun `returns null and touches nothing for a deleted note`() = runTest {
        coEvery { notesRepository.getNote(5L) } returns null

        assertNull(useCase(5L))

        coVerify(exactly = 0) { notesRepository.updateNote(any()) }
        coVerify(exactly = 0) { noteSuggestionsRepository.clearSuggestions(any()) }
    }
}
