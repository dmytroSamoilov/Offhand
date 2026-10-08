@file:OptIn(ExperimentalAtomicApi::class)

package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AiCoreDownloadState
import com.dmytrosamoilov.offhand.core.ai.api.AiCoreDownloadStatus
import com.dmytrosamoilov.offhand.core.data.domain.AppForegroundState
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAiCoreDownloadedUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.RememberModelDownloadProgressUseCase
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
// The percent reached is remembered across starts: a download still pending
// at a cold start with the model missing is reported as abandoned, once.
// A note the system interrupted while the app is already back in front is
// resumed at once, without waiting for the next foreground return.
class PendingNotesCoordinator(
    private val aiCoreDownloadStatus: AiCoreDownloadStatus,
    private val sessionManager: RecordingSessionManager,
    private val appForegroundState: AppForegroundState,
    private val resumeInterruptedNotes: ResumeInterruptedNotesUseCase,
    private val isAiCoreDownloaded: IsAiCoreDownloadedUseCase,
    private val rememberDownloadProgress: RememberModelDownloadProgressUseCase,
    private val analyticsTracker: AnalyticsTracker,
) {

    // Application-lifetime scope: lives as long as the process, never cancelled.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isStarted = AtomicBoolean(false)
    private var downloadStartedAt: TimeMark? = null
    private var rememberedStep: Int? = null

    fun start() {
        if (!isStarted.compareAndSet(expectedValue = false, newValue = true)) return
        scope.launch {
            reportAbandonedDownload()
            aiCoreDownloadStatus.state.collect { state ->
                onDownloadState(state is AiCoreDownloadState.Downloading)
                if (state is AiCoreDownloadState.Downloading) rememberProgress(state.progressPercent)
            }
        }
        scope.launch {
            sessionManager.events.collect { event ->
                if (event is NoteProcessingEvent.Interrupted && appForegroundState.isInForeground.value) {
                    resumeInterruptedNotes()
                }
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
            rememberDownloadProgress.remember(null)
        }
        resumeInterruptedNotes()
    }

    private suspend fun reportAbandonedDownload() {
        val pending = rememberDownloadProgress.pending() ?: return
        if (isAiCoreDownloaded()) {
            rememberDownloadProgress.remember(null)
            return
        }
        analyticsTracker.track(AnalyticsEvents.modelDownloadAbandoned(pending))
        rememberDownloadProgress.remember(null)
    }

    // Written in steps, not on every tick: the value only has to survive a kill.
    private suspend fun rememberProgress(percent: Int) {
        val step = percent / PROGRESS_STEP_PERCENT
        if (rememberedStep == step) return
        rememberedStep = step
        rememberDownloadProgress.remember(percent)
    }

    private companion object {
        const val PROGRESS_STEP_PERCENT = 10
    }
}
