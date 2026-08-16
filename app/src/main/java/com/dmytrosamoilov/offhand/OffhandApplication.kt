package com.dmytrosamoilov.offhand

import android.app.Application
import android.content.ComponentCallbacks2
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.di.coreAiApiModule
import com.dmytrosamoilov.offhand.core.ai.local.di.coreAiLocalModule
import com.dmytrosamoilov.offhand.core.audio.di.coreAudioModule
import com.dmytrosamoilov.offhand.core.data.di.coreDataModule
import com.dmytrosamoilov.offhand.core.device.di.coreDeviceModule
import com.dmytrosamoilov.offhand.core.security.di.coreSecurityModule
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.dmytrosamoilov.offhand.di.appModule
import com.dmytrosamoilov.offhand.feature.notes.di.featureNotesAndroidModule
import com.dmytrosamoilov.offhand.feature.notes.di.featureNotesModule
import com.dmytrosamoilov.offhand.feature.onboarding.di.featureOnboardingAndroidModule
import com.dmytrosamoilov.offhand.feature.onboarding.di.featureOnboardingModule
import com.dmytrosamoilov.offhand.feature.recording.di.featureRecordingAndroidModule
import com.dmytrosamoilov.offhand.feature.recording.di.featureRecordingModule
import com.dmytrosamoilov.offhand.feature.recording.domain.PendingNotesCoordinator
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.domain.SessionPhase
import com.dmytrosamoilov.offhand.feature.settings.di.featureSettingsModule
import com.dmytrosamoilov.offhand.telemetry.CrashReportingController
import com.dmytrosamoilov.offhand.telemetry.ReleaseLogTree
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import timber.log.Timber

class OffhandApplication : Application(), KoinComponent {

    private lateinit var crashReportingController: CrashReportingController
    private lateinit var pendingNotesCoordinator: PendingNotesCoordinator
    private lateinit var sessionManager: RecordingSessionManager
    private lateinit var modelManager: ModelManager

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@OffhandApplication)
            modules(
                coreAiApiModule,
                coreAiLocalModule,
                coreAudioModule,
                coreDeviceModule,
                coreSecurityModule,
                coreDataModule,
                featureNotesModule,
                featureNotesAndroidModule,
                featureOnboardingModule,
                featureOnboardingAndroidModule,
                featureRecordingModule,
                featureRecordingAndroidModule,
                featureSettingsModule,
                appModule,
            )
        }
        crashReportingController = get()
        pendingNotesCoordinator = get()
        sessionManager = get()
        modelManager = get()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseLogTree(crashlyticsOrNull()))
        }
        Logger.setMinSeverity(if (BuildConfig.DEBUG) Severity.Verbose else Severity.Assert)
        crashReportingController.start()
        pendingNotesCoordinator.start()
    }

    // The loaded LLM engine holds ~3 GB — keeping it resident in the background
    // makes the whole process the low-memory killer's first target.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN && isPipelineIdle()) {
            modelManager.release()
        }
    }

    private fun isPipelineIdle(): Boolean =
        sessionManager.processingNoteIds.value.isEmpty() &&
            sessionManager.session.value.phase == SessionPhase.IDLE

    private fun crashlyticsOrNull(): FirebaseCrashlytics? =
        if (FirebaseApp.getApps(this).isEmpty()) null else FirebaseCrashlytics.getInstance()
}
