package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.ai.api.di.coreAiApiModule
import com.dmytrosamoilov.offhand.core.device.di.coreDeviceModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun sharedIosModules(): List<Module> = listOf(
    coreAiApiModule,
    coreDeviceModule,
)

fun startSharedKoin() {
    startKoin {
        modules(sharedIosModules())
    }
}
