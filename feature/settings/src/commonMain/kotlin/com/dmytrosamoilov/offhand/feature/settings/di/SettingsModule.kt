package com.dmytrosamoilov.offhand.feature.settings.di

import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveNotePresetUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetNotePresetUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.settings.presentation.AboutSupportViewModel
import com.dmytrosamoilov.offhand.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featureSettingsModule = module {
    factoryOf(::ObserveAppLockEnabledUseCase)
    factoryOf(::ObserveDeveloperOptionsUseCase)
    factoryOf(::ObserveDynamicColorUseCase)
    factoryOf(::ObserveNotePresetUseCase)
    factoryOf(::ObserveTelemetryConsentUseCase)
    factoryOf(::SetAppLockEnabledUseCase)
    factoryOf(::SetDeveloperOptionsUseCase)
    factoryOf(::SetDynamicColorUseCase)
    factoryOf(::SetNotePresetUseCase)
    factoryOf(::SetTelemetryConsentUseCase)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AboutSupportViewModel)
}
