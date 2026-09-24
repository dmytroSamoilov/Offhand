package com.dmytrosamoilov.offhand.shared

import androidx.lifecycle.ViewModelStore
import com.dmytrosamoilov.offhand.core.ai.api.AiCoreDownloadStatus
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.common.ModelDownloadController
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.feature.backup.presentation.BackupViewModel
import com.dmytrosamoilov.offhand.feature.notes.presentation.NotesViewModel
import com.dmytrosamoilov.offhand.feature.onboarding.presentation.OnboardingViewModel
import com.dmytrosamoilov.offhand.feature.paywall.presentation.PaywallViewModel
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ResumeInterruptedNotesUseCase
import com.dmytrosamoilov.offhand.feature.recording.presentation.RecordingViewModel
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.settings.presentation.AboutSupportViewModel
import com.dmytrosamoilov.offhand.feature.settings.presentation.NoteStyleEditorViewModel
import com.dmytrosamoilov.offhand.feature.settings.presentation.NoteStylesViewModel
import com.dmytrosamoilov.offhand.feature.settings.presentation.SettingsViewModel
import com.dmytrosamoilov.offhand.feature.settings.presentation.SharedAudioImportViewModel
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

object SharedGraph {

    fun rootViewModel(): IosRootViewModel = KoinPlatform.getKoin().get()

    fun notesViewModel(): NotesViewModel = KoinPlatform.getKoin().get()

    fun recordingViewModel(): RecordingViewModel = KoinPlatform.getKoin().get()

    fun onboardingViewModel(): OnboardingViewModel = KoinPlatform.getKoin().get()

    fun settingsViewModel(): SettingsViewModel = KoinPlatform.getKoin().get()

    fun sharedAudioImportViewModel(): SharedAudioImportViewModel = KoinPlatform.getKoin().get()

    fun aboutSupportViewModel(): AboutSupportViewModel = KoinPlatform.getKoin().get()

    fun backupViewModel(): BackupViewModel = KoinPlatform.getKoin().get()

    fun noteStylesViewModel(): NoteStylesViewModel = KoinPlatform.getKoin().get()

    fun paywallViewModel(): PaywallViewModel = KoinPlatform.getKoin().get()

    fun proUpgradeGate(): ProUpgradeGate = KoinPlatform.getKoin().get()

    fun refreshProStatus() {
        KoinPlatform.getKoin().get<ProStore>().refresh()
    }

    fun noteStyleEditor(styleId: Long): NoteStyleEditorHandle = NoteStyleEditorHandle(styleId)

    fun sessionManager(): RecordingSessionManager = KoinPlatform.getKoin().get()

    // Called from the iOS background resume task, outside any screen.
    suspend fun resumeInterruptedNotes() = KoinPlatform.getKoin().get<ResumeInterruptedNotesUseCase>().invoke()

    fun modelManager(): ModelManager = KoinPlatform.getKoin().get()

    fun aiCoreDownloadStatus(): AiCoreDownloadStatus = KoinPlatform.getKoin().get()

    fun observeTelemetryConsent(): ObserveTelemetryConsentUseCase = KoinPlatform.getKoin().get()

    fun startModelDownload() {
        KoinPlatform.getKoin().get<ModelDownloadController>().start()
    }

    // Called from the app delegate when iOS wakes the app for a background download.
    fun handleBackgroundDownloadEvents(identifier: String, completion: () -> Unit) {
        KoinPlatform.getKoin().get<IosFileDownloader>().handleBackgroundEvents(identifier, completion)
    }
}

class NoteStyleEditorHandle(styleId: Long) {

    private val store = ViewModelStore()

    val viewModel: NoteStyleEditorViewModel = KoinPlatform.getKoin()
        .get<NoteStyleEditorViewModel> { parametersOf(styleId) }
        .also { store.put(EDITOR_KEY, it) }

    fun close() = store.clear()

    private companion object {
        const val EDITOR_KEY = "noteStyleEditor"
    }
}
