package com.dmytrosamoilov.offhand.telemetry

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class ReleaseLogTree(
    private val crashlytics: FirebaseCrashlytics?,
) : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Log.println(priority, tag ?: DEFAULT_TAG, message)
        if (crashlytics == null) return
        crashlytics.log("${tag ?: DEFAULT_TAG}: $message")
        if (t != null && priority >= Log.ERROR) {
            crashlytics.recordException(t)
        }
    }

    private companion object {
        const val DEFAULT_TAG = "Offhand"
    }
}
