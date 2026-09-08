package com.dmytrosamoilov.offhand.di

import com.dmytrosamoilov.offhand.testing.fakes.smokeFakesModule
import org.koin.core.module.Module

object FlavorModules {
    val overrides: List<Module> = listOf(smokeFakesModule)
}
