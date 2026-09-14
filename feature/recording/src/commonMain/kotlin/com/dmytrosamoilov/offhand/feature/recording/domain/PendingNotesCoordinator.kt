@file:OptIn(ExperimentalAtomicApi::class)

package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AiCoreDownloadState
import com.dmytrosamoilov.offhand.core.ai.api.AiCoreDownloadStatus
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAiCoreDownloadedUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ResumeInterruptedNotesUseCase
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Reacts to the model download finishing: notes waiting for the AI core are
// picked up, and the download time is reported (measured from when this
// process saw the download running, so a restart mid-download shortens it).
class PendingNotesCoordinator(
    private val aiCoreDownloadStatus: AiCoreDownloadStatus,
    private val resumeInterruptedNotes: ResumeInterruptedNotesUseCase,
    private val isAiCoreDownloaded: IsAiCoreDownloadedUseCase,
    private val analyticsTracker: AnalyticsTracker,
) {

    // Application-lifetime scope: lives as long as the process, never cancelled.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isStarted = AtomicBoolean(false)
    private var downloadStartedAt: TimeMark? = null

    fun start() {
        if (!isStarted.compareAndSet(expectedValue = false, newValue = true)) return
        scope.launch {
            aiCoreDownloadStatus.state.collect { state ->
                onDownloadState(state is AiCoreDownloadState.Downloading)
            }
        }
    }

    private suspend fun onDownloadState(isDownloading: Boolean) {
        val startedAt = downloadStartedAt
        when {
            isDownloading && startedAt == null -> downloadStartedAt = TimeSource.Monotonic.markNow()
            !isDownloading && startedAt != null -> {
                downloadStartedAt = null
                onDownloadFinished(startedAt)
            }
        }
    }

    private suspend fun onDownloadFinished(startedAt: TimeMark) {
        if (isAiCoreDownloaded()) {
            analyticsTracker.track(AnalyticsEvents.modelDownloaded(startedAt.elapsedNow().inWholeMilliseconds))
        }
        resumeInterruptedNotes()
    }
}
