package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker

class SettleRememberedModelDownloadUseCase(
    private val isAiCoreDownloaded: IsAiCoreDownloadedUseCase,
    private val rememberDownloadProgress: RememberModelDownloadProgressUseCase,
    private val analyticsTracker: AnalyticsTracker,
) {
    suspend operator fun invoke() {
        val pending = rememberDownloadProgress.pending() ?: return
        analyticsTracker.track(outcomeOf(pending))
        rememberDownloadProgress.remember(null)
    }

    private suspend fun outcomeOf(pendingPercent: Int) =
        if (isAiCoreDownloaded()) {
            AnalyticsEvents.modelDownloadedWhileAway()
        } else {
            AnalyticsEvents.modelDownloadAbandoned(pendingPercent)
        }
}
