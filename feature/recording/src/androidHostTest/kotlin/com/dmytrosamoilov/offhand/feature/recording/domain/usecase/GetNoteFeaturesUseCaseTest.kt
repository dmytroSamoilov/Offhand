package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferences
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.device.DeviceTierProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetNoteFeaturesUseCaseTest {

    private val preferences: UserPreferences = mockk {
        every { transcriptionEnabled } returns true
        every { summaryEnabled } returns true
    }
    private val repository: UserPreferencesRepository = mockk { every { this@mockk.preferences } returns flowOf(this@GetNoteFeaturesUseCaseTest.preferences) }

    private fun useCase(writesSummaries: Boolean) =
        GetNoteFeaturesUseCase(repository, mockk<DeviceTierProvider> { every { writesSummaries() } returns writesSummaries })

    @Test
    fun `a phone that writes summaries follows the switches`() = runTest {
        assertTrue(useCase(writesSummaries = true)().writesSummary)
    }

    @Test
    fun `a transcripts-only phone never writes a summary, whatever the switch says`() = runTest {
        val features = useCase(writesSummaries = false)()

        assertFalse(features.writesSummary)
        assertFalse(features.isSummaryAvailable)
        assertTrue(features.isTranscriptionEnabled)
    }
}
