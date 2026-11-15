package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Folder
import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetFolderStyleUseCaseTest {

    private val folders: FoldersRepository = mockk()
    private val entitlements: ProStatusRepository = mockk()
    private val useCase = GetFolderStyleUseCase(folders, entitlements)
    private val meetings = Folder(id = 3, name = "Meetings", createdAtEpochMs = 0, style = NoteStyleRef.BuiltIn(NotePreset.MEETING))

    @Test
    fun `a folder with its own style hands it out to a Pro user`() = runTest {
        every { folders.observeFolders() } returns flowOf(listOf(meetings))
        every { entitlements.observeStatus() } returns flowOf(ProStatus.LIFETIME)

        assertEquals(NoteStyleRef.BuiltIn(NotePreset.MEETING), useCase(3L))
    }

    @Test
    fun `a folder style is ignored while the plan is free`() = runTest {
        every { folders.observeFolders() } returns flowOf(listOf(meetings))
        every { entitlements.observeStatus() } returns flowOf(ProStatus.FREE)

        assertNull(useCase(3L))
    }

    @Test
    fun `no folder or a folder without a style gives nothing`() = runTest {
        every { folders.observeFolders() } returns flowOf(listOf(meetings.copy(style = null)))

        assertNull(useCase(null))
        assertNull(useCase(3L))
    }
}
