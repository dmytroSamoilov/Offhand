package com.dmytrosamoilov.offhand.core.common

import kotlinx.coroutines.flow.StateFlow

// Whether the current network is one the user is not paying for per byte.
interface NetworkMonitor {

    val isUnmetered: StateFlow<Boolean>
}
