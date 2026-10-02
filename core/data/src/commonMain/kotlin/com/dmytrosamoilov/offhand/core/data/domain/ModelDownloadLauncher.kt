package com.dmytrosamoilov.offhand.core.data.domain

import com.dmytrosamoilov.offhand.core.common.ModelDownloadController
import com.dmytrosamoilov.offhand.core.common.ModelDownloadPolicy
import com.dmytrosamoilov.offhand.core.common.NetworkMonitor
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import kotlinx.coroutines.flow.first

// The one place the on-device AI download is started from, so every start is
// reported with the network it ran on and the mobile-data rule is applied once.
class ModelDownloadLauncher(
    private val controller: ModelDownloadController,
    private val policy: ModelDownloadPolicy,
    private val networkMonitor: NetworkMonitor,
    private val analyticsTracker: AnalyticsTracker,
) {

    fun startIfAllowed(): Boolean {
        if (!policy.canStartNow()) return false
        start()
        return true
    }

    fun startOnMobileData() {
        policy.allowMobileData()
        start()
    }

    suspend fun startWhenAllowed() {
        policy.canStart.first { it }
        start()
    }

    private fun start() {
        val network = if (networkMonitor.isUnmetered.value) NETWORK_WIFI else NETWORK_MOBILE
        analyticsTracker.track(AnalyticsEvents.modelDownloadStarted(network))
        controller.start()
    }

    private companion object {
        const val NETWORK_WIFI = "wifi"
        const val NETWORK_MOBILE = "mobile"
    }
}
