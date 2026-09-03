package com.dmytrosamoilov.offhand.core.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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

    init {
        // Lifecycle observers must be registered on the main thread; Koin may
        // construct this singleton elsewhere.
        Handler(Looper.getMainLooper()).post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    markLocked()
                }
            })
        }
    }

    override fun markUnlocked() {
        mutableLockState.value = AppLockState.UNLOCKED
    }

    override fun markLocked() {
        mutableLockState.value = AppLockState.LOCKED
    }
}
