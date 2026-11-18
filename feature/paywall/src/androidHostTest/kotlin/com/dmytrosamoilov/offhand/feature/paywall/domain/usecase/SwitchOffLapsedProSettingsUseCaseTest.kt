package com.dmytrosamoilov.offhand.feature.paywall.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferences
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SwitchOffLapsedProSettingsUseCaseTest {

    private val proStatus: ProStatusRepository = mockk()
    private val preferences: UserPreferencesRepository = mockk(relaxed = true)
    private val useCase = SwitchOffLapsedProSettingsUseCase(proStatus, preferences)

    @Test
    fun `skip silence is switched off once pro has lapsed`() = runTest {
        given(status = ProStatus.FREE, skipSilence = true)

        useCase()

        coVerify { preferences.setSkipSilenceEnabled(false) }
    }

    @Test
    fun `skip silence stays on while the plan is pro`() = runTest {
        given(status = ProStatus.INCLUDED, skipSilence = true)

        useCase()

        coVerify(exactly = 0) { preferences.setSkipSilenceEnabled(any()) }
    }

    @Test
    fun `nothing is written when skip silence is already off`() = runTest {
        given(status = ProStatus.FREE, skipSilence = false)

        useCase()

        coVerify(exactly = 0) { preferences.setSkipSilenceEnabled(any()) }
    }

    private fun given(status: ProStatus, skipSilence: Boolean) {
        every { proStatus.observeStatus() } returns flowOf(status)
        every { preferences.preferences } returns flowOf(preferencesWith(skipSilence))
    }

    private fun preferencesWith(skipSilence: Boolean) = mockk<UserPreferences> {
        every { skipSilenceEnabled } returns skipSilence
    }
}
