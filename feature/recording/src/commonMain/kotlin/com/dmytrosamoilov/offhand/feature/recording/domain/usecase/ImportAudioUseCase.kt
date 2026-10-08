package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.AudioImportKind
import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.feature.recording.domain.ImportAllowance
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import kotlinx.coroutines.flow.first

enum class ImportAudioResult {
    STARTED,
    LOCKED,
    ONE_VIDEO_AT_A_TIME,
}

// One gate check per batch: a free user with a free import left and a single
// file goes straight in and spends it; otherwise the paywall opens once and,
// after buying, every staged file continues into the pipeline. A video is
// always imported on its own.
class ImportAudioUseCase(
    private val proUpgradeGate: ProUpgradeGate,
    private val observeImportAllowance: ObserveImportAllowanceUseCase,
    private val userPreferences: UserPreferencesRepository,
    private val recordingProcessController: RecordingProcessController,
    private val sessionManager: RecordingSessionManager,
    private val analyticsTracker: AnalyticsTracker,
) {
    suspend operator fun invoke(sources: List<AudioImportSource>): ImportAudioResult {
        if (sources.isEmpty()) return ImportAudioResult.STARTED
        val videos = sources.count { it.kind == AudioImportKind.VIDEO }
        if (videos > 0 && sources.size > 1) return ImportAudioResult.ONE_VIDEO_AT_A_TIME
        val allowance = observeImportAllowance().first()
        if (!allowance.covers(sources.size) && !proUpgradeGate.requirePro(ProFeature.AUDIO_IMPORT)) return ImportAudioResult.LOCKED
        if (allowance is ImportAllowance.Free && allowance.covers(sources.size)) userPreferences.incrementFreeImportsUsed()
        analyticsTracker.track(AnalyticsEvents.audioImportStarted(sources.size, videos))
        sources.forEach { source ->
            if (!recordingProcessController.importAudio(source)) sessionManager.importAudio(source)
        }
        return ImportAudioResult.STARTED
    }
}
