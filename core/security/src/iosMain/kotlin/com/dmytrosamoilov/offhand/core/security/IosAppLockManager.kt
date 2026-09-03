@file:OptIn(ExperimentalForeignApi::class)

package com.dmytrosamoilov.offhand.core.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSNotificationCenter
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.UIKit.UIApplicationDidEnterBackgroundNotification

class IosAppLockManager : AppLockManager {

    override val isDeviceSecure: Boolean
        get() = LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error = null)

    private val mutableLockState = MutableStateFlow(
        if (isDeviceSecure) AppLockState.LOCKED else AppLockState.UNLOCKED,
    )
    override val lockState: StateFlow<AppLockState> = mutableLockState.asStateFlow()

    init {
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = null,
            usingBlock = { markLocked() },
        )
    }

    override fun markUnlocked() {
        mutableLockState.value = AppLockState.UNLOCKED
    }

    override fun markLocked() {
        mutableLockState.value = AppLockState.LOCKED
    }
}
