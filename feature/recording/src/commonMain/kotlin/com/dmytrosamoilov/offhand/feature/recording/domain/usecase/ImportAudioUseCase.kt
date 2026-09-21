package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager

enum class ImportAudioResult {
    STARTED,
    LOCKED,
}

// One gate check per batch: a free user sees the paywall once and, after
// buying, every staged file continues into the pipeline.
class ImportAudioUseCase(
    private val proUpgradeGate: ProUpgradeGate,
    private val recordingProcessController: RecordingProcessController,
    private val sessionManager: RecordingSessionManager,
    private val analyticsTracker: AnalyticsTracker,
) {
    suspend operator fun invoke(sources: List<AudioImportSource>): ImportAudioResult {
        if (sources.isEmpty()) return ImportAudioResult.STARTED
        if (!proUpgradeGate.requirePro(ProFeature.AUDIO_IMPORT)) return ImportAudioResult.LOCKED
        analyticsTracker.track(AnalyticsEvents.audioImportStarted(sources.size))
        sources.forEach { source ->
            if (!recordingProcessController.importAudio(source)) sessionManager.importAudio(source)
        }
        return ImportAudioResult.STARTED
    }
}
