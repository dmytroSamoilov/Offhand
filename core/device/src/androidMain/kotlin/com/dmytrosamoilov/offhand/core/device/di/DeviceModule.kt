package com.dmytrosamoilov.offhand.core.device.di

import com.dmytrosamoilov.offhand.core.device.AndroidDeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import com.dmytrosamoilov.offhand.core.common.NetworkMonitor
import com.dmytrosamoilov.offhand.core.device.AndroidNetworkMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreDeviceModule = module {
    singleOf(::AndroidDeviceCapabilityChecker) bind DeviceCapabilityChecker::class
    single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
}
