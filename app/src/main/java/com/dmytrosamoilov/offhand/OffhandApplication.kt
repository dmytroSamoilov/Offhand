package com.dmytrosamoilov.offhand

import android.app.Application
import com.dmytrosamoilov.offhand.telemetry.CrashReportingController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class OffhandApplication : Application() {

    @Inject
    lateinit var crashReportingController: CrashReportingController

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        crashReportingController.start()
    }
}
