package com.dmytrosamoilov.offhand.core.security

import android.app.KeyguardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLockState {
    LOCKED,
    UNLOCKED,
}

@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val isDeviceSecure: Boolean
        get() = (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure

    private val mutableLockState = MutableStateFlow(
        if (isDeviceSecure) AppLockState.LOCKED else AppLockState.UNLOCKED,
    )
    val lockState: StateFlow<AppLockState> = mutableLockState.asStateFlow()

    fun markUnlocked() {
        mutableLockState.value = AppLockState.UNLOCKED
    }
}
