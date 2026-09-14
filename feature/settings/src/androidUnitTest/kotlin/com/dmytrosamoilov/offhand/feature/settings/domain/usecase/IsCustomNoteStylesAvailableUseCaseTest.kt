package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class IsCustomNoteStylesAvailableUseCaseTest {

    private val repository: ProStatusRepository = mockk()
    private val useCase = IsCustomNoteStylesAvailableUseCase(repository)

    @Test
    fun `mirrors the pro status and drops repeats`() = runTest {
        every { repository.observeStatus() } returns flowOf(
            ProStatus.FREE,
            ProStatus.FREE,
            ProStatus.LIFETIME,
        )

        assertEquals(listOf(false, true), useCase().toList())
    }
}
