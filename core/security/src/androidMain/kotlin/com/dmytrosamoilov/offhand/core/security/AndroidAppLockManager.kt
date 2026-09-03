package com.dmytrosamoilov.offhand.core.security

import android.app.KeyguardManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidAppLockManager(
    private val context: Context,
) : AppLockManager {

    override val isDeviceSecure: Boolean
        get() = (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure

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
