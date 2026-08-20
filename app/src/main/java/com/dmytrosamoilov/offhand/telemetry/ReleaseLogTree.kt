package com.dmytrosamoilov.offhand.telemetry

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

// Resolved per log rather than captured once: Firebase only exists after consent
// is granted, which can happen long after this tree is planted.
class ReleaseLogTree(
    private val crashlytics: () -> FirebaseCrashlytics?,
) : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Log.println(priority, tag ?: DEFAULT_TAG, message)
        val reporter = crashlytics() ?: return
        reporter.log("${tag ?: DEFAULT_TAG}: $message")
        if (t != null && priority >= Log.ERROR) {
            reporter.recordException(t)
        }
    }

    private companion object {
        const val DEFAULT_TAG = "Offhand"
    }
}
