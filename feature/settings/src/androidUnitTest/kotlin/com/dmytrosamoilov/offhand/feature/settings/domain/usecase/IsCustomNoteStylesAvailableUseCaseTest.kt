package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Entitlements
import com.dmytrosamoilov.offhand.core.data.domain.EntitlementsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class IsCustomNoteStylesAvailableUseCaseTest {

    private val repository: EntitlementsRepository = mockk()
    private val useCase = IsCustomNoteStylesAvailableUseCase(repository)

    @Test
    fun `mirrors the custom styles entitlement and drops repeats`() = runTest {
        every { repository.observeEntitlements() } returns flowOf(
            Entitlements(customStylesUnlocked = false, audioImportUnlocked = true, calendarSuggestionsUnlocked = true),
            Entitlements(customStylesUnlocked = false, audioImportUnlocked = true, calendarSuggestionsUnlocked = true),
            Entitlements(customStylesUnlocked = true, audioImportUnlocked = true, calendarSuggestionsUnlocked = true),
        )

        assertEquals(listOf(false, true), useCase().toList())
    }
}
