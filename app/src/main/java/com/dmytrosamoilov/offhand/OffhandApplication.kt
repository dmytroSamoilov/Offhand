package com.dmytrosamoilov.offhand

import android.app.Application
import android.content.ComponentCallbacks2
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.di.coreAiApiModule
import com.dmytrosamoilov.offhand.core.ai.local.di.coreAiLocalModule
import com.dmytrosamoilov.offhand.core.audio.di.coreAudioModule
import com.dmytrosamoilov.offhand.core.data.di.coreDataModule
import com.dmytrosamoilov.offhand.core.device.di.coreDeviceModule
import com.dmytrosamoilov.offhand.core.security.AppLockManager
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
import com.dmytrosamoilov.offhand.telemetry.FirebaseReporting
import com.dmytrosamoilov.offhand.telemetry.TelemetryController
import com.dmytrosamoilov.offhand.telemetry.ReleaseLogTree
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import timber.log.Timber

class OffhandApplication : Application(), KoinComponent {

    private lateinit var telemetryController: TelemetryController
    private lateinit var pendingNotesCoordinator: PendingNotesCoordinator
    private lateinit var sessionManager: RecordingSessionManager
    private lateinit var appLockManager: AppLockManager
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
        telemetryController = get()
        pendingNotesCoordinator = get()
        sessionManager = get()
        appLockManager = get()
        modelManager = get()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseLogTree { FirebaseReporting.instanceOrNull(this) })
        }
        Logger.setMinSeverity(if (BuildConfig.DEBUG) Severity.Verbose else Severity.Assert)
        telemetryController.start()
        pendingNotesCoordinator.start()
        observeForegroundForLock()
    }

    // Re-lock on the way to the background, except mid-recording: replacing the
    // content with the lock screen tears down the record sheet and strands the
    // live capture. The recording service keeps the audio alive regardless.
    private fun observeForegroundForLock() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                if (sessionManager.session.value.phase != SessionPhase.RECORDING) {
                    appLockManager.markLocked()
                }
            }
        })
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
}
