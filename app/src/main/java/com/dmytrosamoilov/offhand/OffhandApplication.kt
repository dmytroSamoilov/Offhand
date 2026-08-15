package com.dmytrosamoilov.offhand

import android.app.Application
import android.content.ComponentCallbacks2
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.feature.recording.domain.PendingNotesCoordinator
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.domain.SessionPhase
import com.dmytrosamoilov.offhand.telemetry.CrashReportingController
import com.dmytrosamoilov.offhand.telemetry.ReleaseLogTree
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class OffhandApplication : Application() {

    @Inject
    lateinit var crashReportingController: CrashReportingController

    @Inject
    lateinit var pendingNotesCoordinator: PendingNotesCoordinator

    @Inject
    lateinit var sessionManager: RecordingSessionManager

    @Inject
    lateinit var modelManager: ModelManager

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseLogTree(crashlyticsOrNull()))
        }
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
