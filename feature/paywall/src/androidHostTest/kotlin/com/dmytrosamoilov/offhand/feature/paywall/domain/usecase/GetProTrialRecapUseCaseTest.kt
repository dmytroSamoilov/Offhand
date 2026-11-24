package com.dmytrosamoilov.offhand.feature.paywall.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsage
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsageCount
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsageRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetProTrialRecapUseCaseTest {

    private val usage: ProTrialUsageRepository = mockk()
    private val useCase = GetProTrialRecapUseCase(usage)

    @Test
    fun `the three most important uses are kept in order of importance`() = runTest {
        coEvery { usage.read() } returns listOf(
            ProTrialUsageCount(ProTrialUsage.AUDIO_BACKUPS, 9),
            ProTrialUsageCount(ProTrialUsage.IMPORTS, 5),
            ProTrialUsageCount(ProTrialUsage.PDF_EXPORTS, 2),
            ProTrialUsageCount(ProTrialUsage.MEETING_NOTES, 3),
        )

        assertEquals(
            listOf(
                ProTrialUsageCount(ProTrialUsage.MEETING_NOTES, 3),
                ProTrialUsageCount(ProTrialUsage.PDF_EXPORTS, 2),
                ProTrialUsageCount(ProTrialUsage.IMPORTS, 5),
            ),
            useCase(),
        )
    }

    @Test
    fun `nothing used means nothing to show`() = runTest {
        coEvery { usage.read() } returns emptyList()

        assertEquals(emptyList<ProTrialUsageCount>(), useCase())
    }
}
