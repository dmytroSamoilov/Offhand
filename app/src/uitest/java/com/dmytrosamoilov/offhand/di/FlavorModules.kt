package com.dmytrosamoilov.offhand.di

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.dmytrosamoilov.offhand.testing.fakes.SmokeDeviceTier
import com.dmytrosamoilov.offhand.testing.fakes.smokeFakesModule
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module

object FlavorModules {
    val overrides: List<Module> = listOf(smokeFakesModule, smokeLaunchArgumentsModule())

    private fun smokeLaunchArgumentsModule() = module {
        single(createdAtStart = true) { SmokeLaunchArguments(androidApplication()) }
    }
}

private class SmokeLaunchArguments(application: Application) : Application.ActivityLifecycleCallbacks {

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        activity.intent?.getStringExtra(DEVICE_TIER_EXTRA)?.let { SmokeDeviceTier.requested = it }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    private companion object {
        const val DEVICE_TIER_EXTRA = "smokeDeviceTier"
    }
}
