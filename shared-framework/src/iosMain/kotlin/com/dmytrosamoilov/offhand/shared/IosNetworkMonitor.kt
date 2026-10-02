package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.common.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Implemented in Swift over NWPathMonitor; "unmetered" is a satisfied path
// that is neither expensive nor constrained.
interface IosNetworkMonitorBridge {

    fun isUnmetered(): Boolean

    fun observe(onChange: (Boolean) -> Unit)
}

class IosNetworkMonitor(
    bridge: IosNetworkMonitorBridge,
) : NetworkMonitor {

    private val mutableIsUnmetered = MutableStateFlow(bridge.isUnmetered())
    override val isUnmetered: StateFlow<Boolean> = mutableIsUnmetered.asStateFlow()

    init {
        bridge.observe { unmetered -> mutableIsUnmetered.value = unmetered }
    }
}
