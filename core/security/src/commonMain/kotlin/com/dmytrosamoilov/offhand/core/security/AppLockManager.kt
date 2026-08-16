package com.dmytrosamoilov.offhand.core.security

import kotlinx.coroutines.flow.StateFlow

enum class AppLockState {
    LOCKED,
    UNLOCKED,
}

interface AppLockManager {

    val isDeviceSecure: Boolean

    val lockState: StateFlow<AppLockState>

    fun markUnlocked()
}
