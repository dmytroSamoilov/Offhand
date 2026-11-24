package com.dmytrosamoilov.offhand.feature.backup.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EstimateBackupSizeUseCaseTest {

    private fun note(id: Long, audio: String?, status: NoteStatus = NoteStatus.READY) = Note(
        id = id,
        title = "Title",
        body = "Body",
        transcript = "A".repeat(10_000),
        createdAtEpochMs = 0,
        transcriptionTimeMs = null,
        structuringTimeMs = null,
        hardwareBackend = null,
        audioFileName = audio,
        status = status,
    )

    private fun useCase(notes: List<Note>) = EstimateBackupSizeUseCase(
        notesRepository = mockk { every { observeNotes() } returns MutableStateFlow(notes) },
        foldersRepository = mockk { every { observeFolders() } returns MutableStateFlow(emptyList()) },
        customNoteStylesRepository = mockk { every { observeStyles() } returns MutableStateFlow(emptyList()) },
        audioStore = mockk {
            every { pcmSizeOf("a.pcm") } returns 3_200_000L
            every { pcmSizeOf("b.pcm") } throws IllegalStateException()
        },
    )

    @Test
    fun `recordings are counted apart from the text, once per file`() = runTest {
        val estimate = useCase(listOf(note(1, "a.pcm"), note(2, "a.pcm"), note(3, null))).invoke()

        assertTrue(estimate.textBytes > 30_000L)
        assertTrue(estimate.recordingBytes in 3_200_000L..3_201_000L)
    }

    @Test
    fun `notes still being worked on and unreadable audio are left out`() = runTest {
        val estimate = useCase(listOf(note(1, "a.pcm", NoteStatus.PROCESSING), note(2, "b.pcm"))).invoke()

        assertTrue(estimate.recordingBytes < 1_000L)
        assertEquals(estimate.textBytes, useCase(listOf(note(2, null))).invoke().textBytes)
    }
}
