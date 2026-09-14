package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Folder
import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.feature.notes.domain.FolderNameError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderUseCasesTest {

    private val existing = listOf(Folder(id = 7, name = "Work", createdAtEpochMs = 0))
    private val foldersRepository: FoldersRepository = mockk(relaxed = true) {
        every { observeFolders() } returns flowOf(existing)
        coEvery { createFolder(any()) } returns 42L
    }
    private val notesRepository: NotesRepository = mockk(relaxed = true)

    @Test
    fun `create stores the normalized name and returns the new id`() = runTest {
        val result = CreateFolderUseCase(foldersRepository)("  Personal  ")

        assertEquals(FolderSaveResult.Saved(42L), result)
        coVerify { foldersRepository.createFolder("Personal") }
    }

    @Test
    fun `create rejects a duplicate without touching the repository`() = runTest {
        val result = CreateFolderUseCase(foldersRepository)("work")

        assertEquals(FolderSaveResult.Rejected(FolderNameError.DUPLICATE), result)
        coVerify(exactly = 0) { foldersRepository.createFolder(any()) }
    }

    @Test
    fun `rename keeps the same id and writes the normalized name`() = runTest {
        val result = RenameFolderUseCase(foldersRepository)(7L, " Work items ")

        assertEquals(FolderSaveResult.Saved(7L), result)
        coVerify { foldersRepository.renameFolder(7L, "Work items") }
    }

    @Test
    fun `rename rejects a blank name`() = runTest {
        val result = RenameFolderUseCase(foldersRepository)(7L, "")

        assertEquals(FolderSaveResult.Rejected(FolderNameError.BLANK), result)
        coVerify(exactly = 0) { foldersRepository.renameFolder(any(), any()) }
    }

    @Test
    fun `move note delegates to the notes repository`() = runTest {
        MoveNoteToFolderUseCase(notesRepository)(noteId = 3L, folderId = null)

        coVerify { notesRepository.moveNoteToFolder(3L, null) }
    }
}
