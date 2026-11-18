package com.dmytrosamoilov.offhand.di

import com.dmytrosamoilov.offhand.core.common.DeviceRegion
import java.util.Locale
import com.dmytrosamoilov.offhand.BuildConfig
import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.ai.local.LiteRtLmManager
import com.dmytrosamoilov.offhand.core.ai.local.LocalAiBackend
import com.dmytrosamoilov.offhand.core.ai.local.WhisperSpeechToText
import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.core.data.domain.AppForegroundState
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsSink
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ClearShareCacheUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.DecideEarlyAdopterProUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ResumeInterruptedNotesUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SweepOrphanedRecordingsUseCase
import com.dmytrosamoilov.offhand.lifecycle.ProcessForegroundState
import com.dmytrosamoilov.offhand.feature.recording.domain.NoteWaveformFiller
import com.dmytrosamoilov.offhand.root.RootViewModel
import com.dmytrosamoilov.offhand.telemetry.TelemetryController
import com.dmytrosamoilov.offhand.telemetry.FirebaseAnalyticsSink
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single { BuildInfo(isDeveloperBuild = BuildConfig.DEBUG || BuildConfig.FLAVOR == DEV_FLAVOR, appVersion = BuildConfig.VERSION_NAME, platform = "android") }
    single { DeviceRegion { Locale.getDefault().country } }
    singleOf(::LiteRtLmManager) bind ModelManager::class
    singleOf(::LocalAiBackend) bind AiBackend::class
    singleOf(::WhisperSpeechToText) bind SpeechToText::class
    singleOf(::TelemetryController)
    singleOf(::FirebaseAnalyticsSink) bind AnalyticsSink::class
    single<AppForegroundState>(createdAtStart = true) { ProcessForegroundState() }
    viewModel {
        RootViewModel(
            observeUserPreferences = get(),
            appLockManager = get(),
            passphraseProvider = get(),
            isAiCoreDownloaded = get(),
            modelDownloadLauncher = get(),
            resumeInterruptedNotes = lazy { get<ResumeInterruptedNotesUseCase>() },
            sweepOrphanedRecordings = lazy { get<SweepOrphanedRecordingsUseCase>() },
            noteWaveformFiller = lazy { get<NoteWaveformFiller>() },
            clearShareCache = get<ClearShareCacheUseCase>(),
            decideEarlyAdopterPro = lazy { get<DecideEarlyAdopterProUseCase>() },
            markEarlyAdopterThanked = get(),
            settleProTrial = get(),
            acknowledgeProTrialNotice = get(),
            keepProTrialInSync = get(),
            proUpgradeGate = get(),
            appForegroundState = get(),
        )
    }
}

private const val DEV_FLAVOR = "dev"
