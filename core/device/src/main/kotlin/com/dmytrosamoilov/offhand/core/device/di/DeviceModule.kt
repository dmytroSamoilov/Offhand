package com.dmytrosamoilov.offhand.core.device.di

import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreDeviceModule = module {
    singleOf(::DeviceCapabilityChecker)
}
