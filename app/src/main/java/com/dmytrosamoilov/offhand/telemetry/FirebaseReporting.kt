package com.dmytrosamoilov.offhand.telemetry

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

// FirebaseInitProvider is removed from the manifest so nothing starts Firebase
// on its own. Initialising it reaches Google's servers regardless of whether
// collection is enabled, which an app promising that nothing leaves the device
// should not do before the user has agreed. iOS holds the same line.
object FirebaseReporting {

    fun instanceOrNull(context: Context): FirebaseCrashlytics? =
        if (isInitialized(context)) FirebaseCrashlytics.getInstance() else null

    // Returns null when the build has no google-services.json values, which is
    // the normal state of a fresh clone.
    fun initialize(context: Context): FirebaseCrashlytics? {
        if (!isInitialized(context) && FirebaseApp.initializeApp(context) == null) return null
        return FirebaseCrashlytics.getInstance()
    }

    private fun isInitialized(context: Context): Boolean =
        FirebaseApp.getApps(context).isNotEmpty()
}
