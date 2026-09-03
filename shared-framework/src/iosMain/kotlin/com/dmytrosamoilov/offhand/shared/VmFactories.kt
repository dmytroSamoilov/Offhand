package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.ai.api.AiCoreDownloadStatus
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.common.ModelDownloadController
import com.dmytrosamoilov.offhand.feature.notes.presentation.NotesViewModel
import com.dmytrosamoilov.offhand.feature.onboarding.presentation.OnboardingViewModel
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.presentation.RecordingViewModel
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.settings.presentation.AboutSupportViewModel
import com.dmytrosamoilov.offhand.feature.settings.presentation.SettingsViewModel
import org.koin.mp.KoinPlatform

object SharedGraph {

    fun rootViewModel(): IosRootViewModel = KoinPlatform.getKoin().get()

    fun notesViewModel(): NotesViewModel = KoinPlatform.getKoin().get()

    fun recordingViewModel(): RecordingViewModel = KoinPlatform.getKoin().get()

    fun onboardingViewModel(): OnboardingViewModel = KoinPlatform.getKoin().get()

    fun settingsViewModel(): SettingsViewModel = KoinPlatform.getKoin().get()

    fun aboutSupportViewModel(): AboutSupportViewModel = KoinPlatform.getKoin().get()

    fun sessionManager(): RecordingSessionManager = KoinPlatform.getKoin().get()

    fun modelManager(): ModelManager = KoinPlatform.getKoin().get()

    fun aiCoreDownloadStatus(): AiCoreDownloadStatus = KoinPlatform.getKoin().get()

    fun observeTelemetryConsent(): ObserveTelemetryConsentUseCase = KoinPlatform.getKoin().get()

    fun startModelDownload() {
        KoinPlatform.getKoin().get<ModelDownloadController>().start()
    }
}
