package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvent
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettleRememberedModelDownloadUseCaseTest {

    private val tracked = mutableListOf<AnalyticsEvent>()
    private val analyticsTracker = object : AnalyticsTracker {
        override fun track(event: AnalyticsEvent) {
            tracked += event
        }
    }
    private val isAiCoreDownloaded: IsAiCoreDownloadedUseCase = mockk {
        coEvery { this@mockk.invoke() } returns false
    }
    private val rememberDownloadProgress: RememberModelDownloadProgressUseCase = mockk(relaxed = true) {
        coEvery { pending() } returns 40
    }
    private val useCase = SettleRememberedModelDownloadUseCase(
        isAiCoreDownloaded,
        rememberDownloadProgress,
        analyticsTracker,
    )

    @Test
    fun `nothing is reported when no download was remembered`() = runTest {
        coEvery { rememberDownloadProgress.pending() } returns null

        useCase()

        assertEquals(emptyList<AnalyticsEvent>(), tracked)
        coVerify(exactly = 0) { rememberDownloadProgress.remember(any()) }
    }

    @Test
    fun `a remembered download with the model missing is reported as abandoned`() = runTest {
        useCase()

        assertEquals(listOf(AnalyticsEvent("model_download_abandoned", mapOf("percent" to 40L))), tracked)
        coVerify { rememberDownloadProgress.remember(null) }
    }

    @Test
    fun `a remembered download with the model on disk is reported as downloaded`() = runTest {
        coEvery { isAiCoreDownloaded() } returns true

        useCase()

        assertEquals(listOf(AnalyticsEvent("model_downloaded")), tracked)
        coVerify { rememberDownloadProgress.remember(null) }
    }
}
