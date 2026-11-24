package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Folder
import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveFolderNameUseCaseTest {

    private val folders: FoldersRepository = mockk()
    private val useCase = ObserveFolderNameUseCase(folders)

    @Test
    fun `a folder id gives that folder's name`() = runTest {
        every { folders.observeFolders() } returns flowOf(listOf(Folder(id = 3, name = "Work", createdAtEpochMs = 0)))

        assertEquals("Work", useCase(3L).first())
    }

    @Test
    fun `no folder or a deleted folder gives no name`() = runTest {
        every { folders.observeFolders() } returns flowOf(emptyList())

        assertNull(useCase(null).first())
        assertNull(useCase(3L).first())
    }
}
