package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ResumeInterruptedNotesUseCaseTest {

    private val recordingProcessController: RecordingProcessController = mockk()
    private val notesRepository: NotesRepository = mockk()
    private val sessionManager: RecordingSessionManager = mockk {
        every { processingNoteIds } returns MutableStateFlow(emptySet())
        every { activeRecordingNoteId } returns MutableStateFlow(null)
    }
    private val failNote: FailNoteUseCase = mockk()
    private val isAiCoreDownloaded: IsAiCoreDownloadedUseCase = mockk()
    private val audioStore: EncryptedAudioStore = mockk {
        every { pcmSizeOf(any()) } returns 0L
    }

    private val useCase = ResumeInterruptedNotesUseCase(
        recordingProcessController = recordingProcessController,
        notesRepository = notesRepository,
        sessionManager = sessionManager,
        failNote = failNote,
        isAiCoreDownloaded = isAiCoreDownloaded,
        audioStore = audioStore,
    )

    @Before
    fun setUp() {
        every { recordingProcessController.retryNote(any(), any()) } returns true
        every { recordingProcessController.restructureNote(any(), any()) } returns true
        coEvery { failNote(any()) } returns true
        coEvery { isAiCoreDownloaded() } returns true
        coEvery { notesRepository.deleteNote(any()) } just Runs
    }

    private fun note(
        id: Long,
        status: NoteStatus,
        audioFileName: String? = "note-$id.pcm.enc",
        createdAtEpochMs: Long = 0,
        transcript: String = "",
        durationMs: Long? = null,
    ) = Note(
        id = id,
        title = "Note $id",
        body = "",
        transcript = transcript,
        createdAtEpochMs = createdAtEpochMs,
        transcriptionTimeMs = null,
        structuringTimeMs = null,
        hardwareBackend = null,
        audioFileName = audioFileName,
        durationMs = durationMs,
        status = status,
    )

    @Test
    fun `stuck processing note with audio is re-transcribed`() = runTest {
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(1, NoteStatus.PROCESSING), note(2, NoteStatus.READY)),
        )

        useCase()

        verify { recordingProcessController.retryNote(1, "note-1.pcm.enc") }
        verify(exactly = 1) { recordingProcessController.retryNote(any(), any()) }
    }

    @Test
    fun `stuck note with a saved transcript is restructured instead of re-transcribed`() = runTest {
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(1, NoteStatus.PROCESSING, transcript = "already transcribed")),
        )

        useCase()

        verify { recordingProcessController.restructureNote(1, NotePreset.DEFAULT) }
        verify(exactly = 0) { recordingProcessController.retryNote(any(), any()) }
    }

    @Test
    fun `stuck note without audio but with transcript is still restructured`() = runTest {
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(
                note(1, NoteStatus.PROCESSING, audioFileName = null, transcript = "saved words"),
            ),
        )

        useCase()

        verify { recordingProcessController.restructureNote(1, NotePreset.DEFAULT) }
        coVerify(exactly = 0) { failNote(any()) }
    }

    @Test
    fun `stuck processing note without audio is marked failed`() = runTest {
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(1, NoteStatus.PROCESSING, audioFileName = null)),
        )

        useCase()

        coVerify { failNote(1) }
        verify(exactly = 0) { recordingProcessController.retryNote(any(), any()) }
    }

    @Test
    fun `actively processing notes are left alone`() = runTest {
        every { sessionManager.processingNoteIds } returns MutableStateFlow(setOf(1L))
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(1, NoteStatus.PROCESSING)),
        )

        useCase()

        verify(exactly = 0) { recordingProcessController.retryNote(any(), any()) }
        coVerify(exactly = 0) { failNote(any()) }
    }

    @Test
    fun `does nothing while the ai core is not downloaded`() = runTest {
        coEvery { isAiCoreDownloaded() } returns false
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(1, NoteStatus.PROCESSING)),
        )

        useCase()

        verify(exactly = 0) { recordingProcessController.retryNote(any(), any()) }
        coVerify(exactly = 0) { failNote(any()) }
    }

    @Test
    fun `stale recording note with a transcript is restructured`() = runTest {
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(3, NoteStatus.RECORDING, transcript = "words from a killed session")),
        )

        useCase()

        verify { recordingProcessController.restructureNote(3, NotePreset.DEFAULT) }
        coVerify(exactly = 0) { notesRepository.deleteNote(any()) }
    }

    @Test
    fun `stale recording note with audio only is re-transcribed`() = runTest {
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(3, NoteStatus.RECORDING)),
        )

        useCase()

        verify { recordingProcessController.retryNote(3, "note-3.pcm.enc") }
    }

    @Test
    fun `stale empty recording note is deleted`() = runTest {
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(3, NoteStatus.RECORDING, audioFileName = null)),
        )

        useCase()

        coVerify { notesRepository.deleteNote(3) }
        coVerify(exactly = 0) { failNote(any()) }
        verify(exactly = 0) { recordingProcessController.retryNote(any(), any()) }
    }

    @Test
    fun `missing duration is backfilled from the audio size before resuming`() = runTest {
        every { audioStore.pcmSizeOf("note-3.pcm.enc") } returns 320_000L
        coEvery { notesRepository.updateNote(any()) } just Runs
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(3, NoteStatus.RECORDING, transcript = "recovered words")),
        )

        useCase()

        coVerify {
            notesRepository.updateNote(
                withArg { updated -> assertEquals(10_000L, updated.durationMs) },
            )
        }
        verify { recordingProcessController.restructureNote(3, NotePreset.DEFAULT) }
    }

    @Test
    fun `existing duration is left untouched during resume`() = runTest {
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(1, NoteStatus.PROCESSING, transcript = "words", durationMs = 5_000L)),
        )

        useCase()

        coVerify(exactly = 0) { notesRepository.updateNote(any()) }
        verify { recordingProcessController.restructureNote(1, NotePreset.DEFAULT) }
    }

    @Test
    fun `note of the live recording session is left alone`() = runTest {
        every { sessionManager.activeRecordingNoteId } returns MutableStateFlow(3L)
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(3, NoteStatus.RECORDING, transcript = "still being recorded")),
        )

        useCase()

        verify(exactly = 0) { recordingProcessController.restructureNote(any(), any()) }
        verify(exactly = 0) { recordingProcessController.retryNote(any(), any()) }
        coVerify(exactly = 0) { notesRepository.deleteNote(any()) }
    }

    @Test
    fun `pending notes are queued oldest first`() = runTest {
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(
                note(2, NoteStatus.PROCESSING, createdAtEpochMs = 2_000),
                note(1, NoteStatus.PROCESSING, createdAtEpochMs = 1_000),
            ),
        )

        useCase()

        verifyOrder {
            recordingProcessController.retryNote(1, "note-1.pcm.enc")
            recordingProcessController.retryNote(2, "note-2.pcm.enc")
        }
    }

    @Test
    fun `falls back to in-process retry when the service refuses to start`() = runTest {
        every { recordingProcessController.retryNote(any(), any()) } returns false
        every { sessionManager.retryNote(any(), any()) } just Runs
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(1, NoteStatus.PROCESSING)),
        )

        useCase()

        verify { sessionManager.retryNote(1, "note-1.pcm.enc") }
    }

    @Test
    fun `falls back to in-process restructure when the service refuses to start`() = runTest {
        every { recordingProcessController.restructureNote(any(), any()) } returns false
        every { sessionManager.restructureNote(any(), any()) } just Runs
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(1, NoteStatus.PROCESSING, transcript = "already transcribed")),
        )

        useCase()

        verify { sessionManager.restructureNote(1, NotePreset.DEFAULT) }
    }
}
