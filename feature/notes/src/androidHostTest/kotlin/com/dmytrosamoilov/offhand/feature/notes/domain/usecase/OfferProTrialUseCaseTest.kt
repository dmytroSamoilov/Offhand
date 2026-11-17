package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialNotice
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OfferProTrialUseCaseTest {

    private val trialRepository: ProTrialRepository = mockk {
        coEvery { isNoticeSeen(ProTrialNotice.OFFERED) } returns false
        coJustRun { markNoticeSeen(ProTrialNotice.OFFERED) }
    }
    private val proStatusRepository: ProStatusRepository = mockk()
    private val proUpgradeGate: ProUpgradeGate = mockk { justRun { showTrialOffer() } }
    private val useCase = OfferProTrialUseCase(trialRepository, proStatusRepository, proUpgradeGate)

    @Test
    fun `a user on the trial is shown the offer once`() = runTest {
        every { proStatusRepository.observeStatus() } returns flowOf(ProStatus.trial(endsAtMs = 1L))

        useCase()

        coVerify { trialRepository.markNoticeSeen(ProTrialNotice.OFFERED) }
        verify(exactly = 1) { proUpgradeGate.showTrialOffer() }
    }

    @Test
    fun `the offer is never shown twice`() = runTest {
        coEvery { trialRepository.isNoticeSeen(ProTrialNotice.OFFERED) } returns true

        useCase()

        verify(exactly = 0) { proUpgradeGate.showTrialOffer() }
        coVerify(exactly = 0) { trialRepository.markNoticeSeen(any()) }
    }

    @Test
    fun `a buyer is not offered the trial and is not asked again later`() = runTest {
        every { proStatusRepository.observeStatus() } returns flowOf(ProStatus.LIFETIME)

        useCase()

        verify(exactly = 0) { proUpgradeGate.showTrialOffer() }
        coVerify { trialRepository.markNoticeSeen(ProTrialNotice.OFFERED) }
    }

    @Test
    fun `a free user whose trial has ended is not offered it`() = runTest {
        every { proStatusRepository.observeStatus() } returns flowOf(ProStatus.FREE)

        useCase()

        verify(exactly = 0) { proUpgradeGate.showTrialOffer() }
    }
}
