@file:OptIn(ExperimentalForeignApi::class)

package com.dmytrosamoilov.offhand.core.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication

class IosAppLockManager : AppLockManager {

    override val isDeviceSecure: Boolean
        get() = LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error = null)

    private val mutableLockState = MutableStateFlow(
        if (isDeviceSecure) AppLockState.LOCKED else AppLockState.UNLOCKED,
    )
    override val lockState: StateFlow<AppLockState> = mutableLockState.asStateFlow()

    override fun markUnlocked() {
        mutableLockState.value = AppLockState.UNLOCKED
    }

    override fun markLocked() {
        mutableLockState.value = AppLockState.LOCKED
    }
}
