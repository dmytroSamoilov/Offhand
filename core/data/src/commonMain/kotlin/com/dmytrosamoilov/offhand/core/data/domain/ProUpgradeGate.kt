package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.StateFlow

// Feature ViewModels call requirePro() before a Pro-only action; the root
// screen shows the paywall while requestedFeature is set and reports back
// through onPaywallClosed(), after which requirePro() answers with the status
// the store reports now.
interface ProUpgradeGate {

    val requestedFeature: StateFlow<ProFeature?>

    suspend fun requirePro(feature: ProFeature): Boolean

    fun onPaywallClosed()
}
