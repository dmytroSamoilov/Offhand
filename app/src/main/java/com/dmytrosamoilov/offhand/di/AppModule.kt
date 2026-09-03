package com.dmytrosamoilov.offhand.di

import com.dmytrosamoilov.offhand.BuildConfig
import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.ai.local.LiteRtLmManager
import com.dmytrosamoilov.offhand.core.ai.local.LocalAiBackend
import com.dmytrosamoilov.offhand.core.ai.local.WhisperSpeechToText
import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ClearShareCacheUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ResumeInterruptedNotesUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SweepOrphanedRecordingsUseCase
import com.dmytrosamoilov.offhand.root.RootViewModel
import com.dmytrosamoilov.offhand.telemetry.TelemetryController
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single { BuildInfo(isDebugBuild = BuildConfig.DEBUG) }
    singleOf(::LiteRtLmManager) bind ModelManager::class
    singleOf(::LocalAiBackend) bind AiBackend::class
    singleOf(::WhisperSpeechToText) bind SpeechToText::class
    singleOf(::TelemetryController)
    viewModel {
        RootViewModel(
            observeUserPreferences = get(),
            appLockManager = get(),
            passphraseProvider = get(),
            modelManager = get(),
            modelDownloadController = get(),
            resumeInterruptedNotes = lazy { get<ResumeInterruptedNotesUseCase>() },
            sweepOrphanedRecordings = lazy { get<SweepOrphanedRecordingsUseCase>() },
            clearShareCache = get<ClearShareCacheUseCase>(),
        )
    }
}
