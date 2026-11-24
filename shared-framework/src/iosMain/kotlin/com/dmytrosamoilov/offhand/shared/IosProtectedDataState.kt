package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.data.domain.ProtectedDataState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidFinishLaunchingNotification
import platform.UIKit.UIApplicationProtectedDataDidBecomeAvailable
import platform.UIKit.UIApplicationProtectedDataWillBecomeUnavailable

// Created with the Koin graph on the main thread, like IosAppForegroundState,
// so the notifications cover every later lock and unlock. That is before the
// app has finished launching, when iOS still reports the data as unavailable
// on an unlocked device and no notification follows, so the answer is read
// again once the launch has finished and whenever the app becomes active.
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
        listOf(UIApplicationDidFinishLaunchingNotification, UIApplicationDidBecomeActiveNotification).forEach { name ->
            center.addObserverForName(name, null, NSOperationQueue.mainQueue) {
                mutableIsAvailable.value = UIApplication.sharedApplication.protectedDataAvailable
            }
        }
    }
}
