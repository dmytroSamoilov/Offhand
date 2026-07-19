package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import android.content.Context
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.service.RecordingService
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class ResumeInterruptedNotesUseCaseTest {

    private val context: Context = mockk(relaxed = true)
    private val notesRepository: NotesRepository = mockk()
    private val sessionManager: RecordingSessionManager = mockk {
        every { processingNoteIds } returns MutableStateFlow(emptySet())
    }
    private val failNote: FailNoteUseCase = mockk()

    private val useCase = ResumeInterruptedNotesUseCase(
        context = context,
        notesRepository = notesRepository,
        sessionManager = sessionManager,
        failNote = failNote,
    )

    @Before
    fun setUp() {
        mockkObject(RecordingService.Companion)
        every { RecordingService.retryNote(any(), any(), any()) } just Runs
        coEvery { failNote(any()) } returns true
    }

    @After
    fun tearDown() {
        unmockkObject(RecordingService.Companion)
    }

    private fun note(
        id: Long,
        status: NoteStatus,
        audioFileName: String? = "note-$id.pcm.enc",
    ) = Note(
        id = id,
        title = "Note $id",
        body = "",
        transcript = "",
        createdAtEpochMs = 0,
        transcriptionTimeMs = null,
        structuringTimeMs = null,
        hardwareBackend = null,
        audioFileName = audioFileName,
        status = status,
    )

    @Test
    fun `stuck processing note with audio is re-transcribed`() = runTest {
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(1, NoteStatus.PROCESSING), note(2, NoteStatus.READY)),
        )

        useCase()

        verify { RecordingService.retryNote(context, 1, "note-1.pcm.enc") }
        verify(exactly = 1) { RecordingService.retryNote(any(), any(), any()) }
    }

    @Test
    fun `stuck processing note without audio is marked failed`() = runTest {
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(1, NoteStatus.PROCESSING, audioFileName = null)),
        )

        useCase()

        coVerify { failNote(1) }
        verify(exactly = 0) { RecordingService.retryNote(any(), any(), any()) }
    }

    @Test
    fun `actively processing notes are left alone`() = runTest {
        every { sessionManager.processingNoteIds } returns MutableStateFlow(setOf(1L))
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(1, NoteStatus.PROCESSING)),
        )

        useCase()

        verify(exactly = 0) { RecordingService.retryNote(any(), any(), any()) }
        coVerify(exactly = 0) { failNote(any()) }
    }

    @Test
    fun `runs only once per process`() = runTest {
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(1, NoteStatus.PROCESSING)),
        )

        useCase()
        useCase()

        verify(exactly = 1) { RecordingService.retryNote(any(), any(), any()) }
    }
}
