package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.core.data.domain.EntitlementsRepository
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import kotlinx.coroutines.flow.first

enum class ImportAudioResult {
    STARTED,
    LOCKED,
}

class ImportAudioUseCase(
    private val entitlements: EntitlementsRepository,
    private val recordingProcessController: RecordingProcessController,
    private val sessionManager: RecordingSessionManager,
) {
    suspend operator fun invoke(source: AudioImportSource): ImportAudioResult {
        if (!entitlements.observeEntitlements().first().audioImportUnlocked) return ImportAudioResult.LOCKED
        if (!recordingProcessController.importAudio(source)) sessionManager.importAudio(source)
        return ImportAudioResult.STARTED
    }
}
