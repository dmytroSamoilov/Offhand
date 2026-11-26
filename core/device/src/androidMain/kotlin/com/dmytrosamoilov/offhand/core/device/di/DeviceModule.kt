package com.dmytrosamoilov.offhand.core.device.di

import com.dmytrosamoilov.offhand.core.device.AndroidBatteryRestriction
import com.dmytrosamoilov.offhand.core.device.AndroidDeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.device.BatteryRestriction
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.device.DeviceMaker
import com.dmytrosamoilov.offhand.core.device.DeviceTierProvider
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import com.dmytrosamoilov.offhand.core.common.NetworkMonitor
import com.dmytrosamoilov.offhand.core.device.AndroidNetworkMonitor
import android.os.Build
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreDeviceModule = module {
    singleOf(::AndroidDeviceCapabilityChecker) bind DeviceCapabilityChecker::class
    single { DeviceTierProvider(get()) }
    single { DeviceMaker { Build.MANUFACTURER.orEmpty() } }
    single<BatteryRestriction> { AndroidBatteryRestriction(androidContext()) }
    single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
}
