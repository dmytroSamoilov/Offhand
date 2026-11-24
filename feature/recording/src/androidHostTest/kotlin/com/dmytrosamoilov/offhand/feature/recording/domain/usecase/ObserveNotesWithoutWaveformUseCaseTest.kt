package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NoteWaveform
import com.dmytrosamoilov.offhand.core.data.domain.NoteWaveformRepository
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveNotesWithoutWaveformUseCaseTest {

    private val notesRepository: NotesRepository = mockk()
    private val waveformRepository: NoteWaveformRepository = mockk()
    private val useCase = ObserveNotesWithoutWaveformUseCase(notesRepository, waveformRepository)

    @Test
    fun `lists finished recordings that have not been measured`() = runTest {
        givenNotes(
            note(1, NoteStatus.READY),
            note(2, NoteStatus.FAILED),
            note(3, NoteStatus.INTERRUPTED),
        )
        givenWaveforms(2L)

        assertEquals(listOf(1L, 3L), useCase().first().map { it.id })
    }

    @Test
    fun `leaves out a recording that is still being written`() = runTest {
        givenNotes(note(1, NoteStatus.RECORDING, durationMs = null))
        givenWaveforms()

        assertEquals(emptyList<Long>(), useCase().first().map { it.id })
    }

    @Test
    fun `waits for an import to be decoded and takes a saved recording that is processing`() = runTest {
        givenNotes(
            note(1, NoteStatus.PROCESSING, durationMs = null),
            note(2, NoteStatus.PROCESSING, durationMs = 9_000),
        )
        givenWaveforms()

        assertEquals(listOf(2L), useCase().first().map { it.id })
    }

    @Test
    fun `leaves out a note without audio`() = runTest {
        givenNotes(note(1, NoteStatus.READY, audioFileName = null))
        givenWaveforms()

        assertEquals(emptyList<Long>(), useCase().first().map { it.id })
    }

    private fun givenNotes(vararg notes: Note) {
        every { notesRepository.observeNotes() } returns flowOf(notes.toList())
    }

    private fun givenWaveforms(vararg noteIds: Long) {
        every { waveformRepository.observeWaveforms() } returns flowOf(noteIds.map { NoteWaveform(it, listOf(50)) })
    }

    private fun note(
        id: Long,
        status: NoteStatus,
        durationMs: Long? = 5_000,
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
        durationMs = durationMs,
        status = status,
    )
}
