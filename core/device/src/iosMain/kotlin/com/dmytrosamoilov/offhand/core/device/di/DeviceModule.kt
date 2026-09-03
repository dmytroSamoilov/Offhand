package com.dmytrosamoilov.offhand.core.device.di

import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.device.IosDeviceCapabilityChecker
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreDeviceModule = module {
    singleOf(::IosDeviceCapabilityChecker) bind DeviceCapabilityChecker::class
}
