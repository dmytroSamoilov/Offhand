package com.dmytrosamoilov.offhand.telemetry

import java.util.concurrent.atomic.AtomicBoolean

// True only while Firebase exists and Analytics collection is switched on,
// which the TelemetryController does a moment after consent is stored.
object AnalyticsReadiness {

    private val isCollecting = AtomicBoolean(false)

    val isReady: Boolean
        get() = isCollecting.get()

    fun set(isCollecting: Boolean) {
        this.isCollecting.set(isCollecting)
    }
}
