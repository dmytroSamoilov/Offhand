package com.dmytrosamoilov.offhand.core.device.di

import com.dmytrosamoilov.offhand.core.device.AndroidDeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreDeviceModule = module {
    singleOf(::AndroidDeviceCapabilityChecker) bind DeviceCapabilityChecker::class
}
