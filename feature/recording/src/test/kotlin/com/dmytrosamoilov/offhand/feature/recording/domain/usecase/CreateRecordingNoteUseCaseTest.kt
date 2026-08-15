package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import android.content.Context
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.feature.recording.R
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
    private val context: Context = mockk()
    private val useCase = CreateRecordingNoteUseCase(context, notesRepository)

    @Test
    fun `creates a recording note titled with the next recording number`() = runTest {
        coEvery { notesRepository.countNotes() } returns 3
        every {
            context.getString(R.string.recording_default_note_title, 4)
        } returns "Recording 4"
        coEvery { notesRepository.createNote(any()) } returns 42L

        val noteId = useCase("audio.pcm.enc", NotePreset.MEETING)

        assertEquals(42L, noteId)
        coVerify {
            notesRepository.createNote(
                withArg { note ->
                    assertEquals("Recording 4", note.title)
                    assertEquals(NoteStatus.RECORDING, note.status)
                    assertEquals("audio.pcm.enc", note.audioFileName)
                    assertNull(note.durationMs)
                    assertEquals(NotePreset.MEETING, note.preset)
                },
            )
        }
    }

    @Test
    fun `first recording is titled Recording 1`() = runTest {
        coEvery { notesRepository.countNotes() } returns 0
        every {
            context.getString(R.string.recording_default_note_title, 1)
        } returns "Recording 1"
        coEvery { notesRepository.createNote(any()) } returns 1L

        useCase(null, NotePreset.SUMMARY)

        coVerify {
            notesRepository.createNote(withArg { note -> assertEquals("Recording 1", note.title) })
        }
    }
}
