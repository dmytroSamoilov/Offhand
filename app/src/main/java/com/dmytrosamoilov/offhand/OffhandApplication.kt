package com.dmytrosamoilov.offhand

import android.app.Application
import com.dmytrosamoilov.offhand.feature.recording.domain.PendingNotesCoordinator
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

    private fun crashlyticsOrNull(): FirebaseCrashlytics? =
        if (FirebaseApp.getApps(this).isEmpty()) null else FirebaseCrashlytics.getInstance()
}
