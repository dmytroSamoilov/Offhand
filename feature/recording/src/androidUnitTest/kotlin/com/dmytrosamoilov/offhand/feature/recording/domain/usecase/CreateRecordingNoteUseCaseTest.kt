package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.feature.recording.domain.DefaultNoteTitleProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreateRecordingNoteUseCaseTest {

    private val notesRepository: NotesRepository = mockk()
    private val defaultNoteTitleProvider: DefaultNoteTitleProvider = mockk()
    private val useCase = CreateRecordingNoteUseCase(defaultNoteTitleProvider, notesRepository)

    @Test
    fun `creates a recording note titled with the next recording number`() = runTest {
        coEvery { notesRepository.countNotes() } returns 3
        every { defaultNoteTitleProvider.titleFor(4) } returns "Recording 4"
        coEvery { notesRepository.createNote(any()) } returns 42L

        val noteId = useCase("audio.pcm.enc", NoteStyleRef.BuiltIn(NotePreset.MEETING), folderId = 5L)

        assertEquals(42L, noteId)
        coVerify {
            notesRepository.createNote(
                withArg { note ->
                    assertEquals("Recording 4", note.title)
                    assertEquals(NoteStatus.RECORDING, note.status)
                    assertEquals("audio.pcm.enc", note.audioFileName)
                    assertNull(note.durationMs)
                    assertEquals(NoteStyleRef.BuiltIn(NotePreset.MEETING), note.style)
                    assertEquals(5L, note.folderId)
                },
            )
        }
    }

    @Test
    fun `first recording outside a folder is titled Recording 1 and has no folder`() = runTest {
        coEvery { notesRepository.countNotes() } returns 0
        every { defaultNoteTitleProvider.titleFor(1) } returns "Recording 1"
        coEvery { notesRepository.createNote(any()) } returns 1L

        useCase(null, NoteStyleRef.BuiltIn(NotePreset.SUMMARY), folderId = null)

        coVerify {
            notesRepository.createNote(
                withArg { note ->
                    assertEquals("Recording 1", note.title)
                    assertNull(note.folderId)
                },
            )
        }
    }
}
