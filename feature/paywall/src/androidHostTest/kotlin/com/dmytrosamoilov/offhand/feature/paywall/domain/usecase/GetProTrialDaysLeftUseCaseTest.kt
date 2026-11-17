package com.dmytrosamoilov.offhand.feature.paywall.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialState
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetProTrialDaysLeftUseCaseTest {

    private val now = 1_000_000_000_000L
    private val trialRepository: ProTrialRepository = mockk()
    private val useCase = GetProTrialDaysLeftUseCase(trialRepository) { now }

    @Test
    fun `a running trial counts the days that are left, part of a day as a whole one`() = runTest {
        coEvery { trialRepository.currentState() } returns ProTrialState.Active(now + 9.days.inWholeMilliseconds + 3.hours.inWholeMilliseconds)

        assertEquals(10, useCase())
    }

    @Test
    fun `an ended trial has no days left to announce`() = runTest {
        coEvery { trialRepository.currentState() } returns ProTrialState.Ended

        assertNull(useCase())
    }
}
