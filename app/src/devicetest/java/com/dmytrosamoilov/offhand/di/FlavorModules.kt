package com.dmytrosamoilov.offhand.di

import com.dmytrosamoilov.offhand.testing.fakes.deviceTestModule
import org.koin.core.module.Module

object FlavorModules {
    val overrides: List<Module> = listOf(deviceTestModule)
}
