package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SweepOrphanedRecordingsUseCaseTest {

    private val notesRepository: NotesRepository = mockk()
    private val audioStore: EncryptedAudioStore = mockk {
        every { deleteUnreferenced(any(), any()) } returns 0
    }
    private val useCase = SweepOrphanedRecordingsUseCase(notesRepository, audioStore)

    private fun note(id: Long, audioFileName: String?) = Note(
        id = id,
        title = "Note $id",
        body = "",
        transcript = "",
        createdAtEpochMs = 0,
        transcriptionTimeMs = null,
        structuringTimeMs = null,
        hardwareBackend = null,
        audioFileName = audioFileName,
        status = NoteStatus.READY,
    )

    @Test
    fun `keeps every referenced audio file`() = runTest {
        every { notesRepository.observeNotes() } returns flowOf(
            listOf(note(1, "a.pcm.enc"), note(2, null), note(3, "b.pcm.enc")),
        )

        useCase()

        verify { audioStore.deleteUnreferenced(setOf("a.pcm.enc", "b.pcm.enc"), any()) }
    }

    @Test
    fun `sweeps with an empty reference set when there are no notes`() = runTest {
        every { notesRepository.observeNotes() } returns flowOf(emptyList())

        useCase()

        verify { audioStore.deleteUnreferenced(emptySet(), any()) }
    }
}
