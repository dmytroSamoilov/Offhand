package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AiCoreDownloadState
import com.dmytrosamoilov.offhand.core.ai.api.AiCoreDownloadStatus
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ResumeInterruptedNotesUseCase
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class PendingNotesCoordinator @Inject constructor(
    private val aiCoreDownloadStatus: AiCoreDownloadStatus,
    private val resumeInterruptedNotes: ResumeInterruptedNotesUseCase,
) {

    // Application-lifetime scope: lives as long as the process, never cancelled.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isStarted = AtomicBoolean(false)

    fun start() {
        if (!isStarted.compareAndSet(false, true)) return
        scope.launch {
            var wasDownloading = false
            aiCoreDownloadStatus.state.collect { state ->
                val isDownloading = state is AiCoreDownloadState.Downloading
                if (wasDownloading && !isDownloading) {
                    resumeInterruptedNotes()
                }
                wasDownloading = isDownloading
            }
        }
    }
}
