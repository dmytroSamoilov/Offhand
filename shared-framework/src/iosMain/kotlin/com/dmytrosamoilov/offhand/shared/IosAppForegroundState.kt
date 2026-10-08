package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.data.domain.AppForegroundState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillResignActiveNotification

// Created with the Koin graph, on the main thread and before the first
// activation, so the notifications cover every later change.
class IosAppForegroundState : AppForegroundState {

    private val mutableIsInForeground =
        MutableStateFlow(UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive)
    override val isInForeground: StateFlow<Boolean> = mutableIsInForeground.asStateFlow()

    init {
        val center = NSNotificationCenter.defaultCenter
        center.addObserverForName(UIApplicationDidBecomeActiveNotification, null, NSOperationQueue.mainQueue) {
            mutableIsInForeground.value = true
        }
        center.addObserverForName(UIApplicationWillResignActiveNotification, null, NSOperationQueue.mainQueue) {
            mutableIsInForeground.value = false
        }
    }
}
