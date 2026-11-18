package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.data.domain.ProtectedDataState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationProtectedDataDidBecomeAvailable
import platform.UIKit.UIApplicationProtectedDataWillBecomeUnavailable

// Created with the Koin graph on the main thread, like IosAppForegroundState,
// so the notifications cover every later lock and unlock.
class IosProtectedDataState : ProtectedDataState {

    private val mutableIsAvailable = MutableStateFlow(UIApplication.sharedApplication.protectedDataAvailable)
    override val isAvailable: StateFlow<Boolean> = mutableIsAvailable.asStateFlow()

    init {
        val center = NSNotificationCenter.defaultCenter
        center.addObserverForName(UIApplicationProtectedDataDidBecomeAvailable, null, NSOperationQueue.mainQueue) {
            mutableIsAvailable.value = true
        }
        center.addObserverForName(UIApplicationProtectedDataWillBecomeUnavailable, null, NSOperationQueue.mainQueue) {
            mutableIsAvailable.value = false
        }
    }
}
