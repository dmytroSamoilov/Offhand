package com.dmytrosamoilov.offhand.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

// The app's own page in system settings is one tap away from its battery
// setting on every phone; the list of all apps is the fallback.
fun Context.openBatterySettings() {
    val appPage = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    val allApps = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    listOf(appPage, allApps).firstOrNull { intent -> opens(intent) }
}

private fun Context.opens(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
}
