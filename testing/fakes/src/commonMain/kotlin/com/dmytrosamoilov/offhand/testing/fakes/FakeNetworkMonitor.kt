package com.dmytrosamoilov.offhand.testing.fakes

import com.dmytrosamoilov.offhand.core.common.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// The smoke flows run on Wi-Fi as far as the app is concerned, whatever the
// emulator reports.
class FakeNetworkMonitor : NetworkMonitor {
    override val isUnmetered: StateFlow<Boolean> = MutableStateFlow(true)
}
