package com.dmytrosamoilov.offhand.core.data.billing

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

// Play's purchase flow needs the resumed Activity, which no repository may
// hold; this keeps a weak pointer that the Application refreshes.
class ForegroundActivityHolder : Application.ActivityLifecycleCallbacks {

    private var current: WeakReference<Activity>? = null

    val activity: Activity?
        get() = current?.get()

    override fun onActivityResumed(activity: Activity) {
        current = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (current?.get() === activity) current = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
