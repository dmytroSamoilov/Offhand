package com.dmytrosamoilov.offhand.feature.settings.di

import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.DeleteCustomNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.DraftNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.GetCustomNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.IsCustomNoteStylesAvailableUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveCustomNoteStylesUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveProOverrideUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveProStatusUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveNoteFeaturesUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveSmartSuggestionsEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetNoteFeatureEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.RedeemProCodeUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SaveCustomNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetProOverrideUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetSmartSuggestionsEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.settings.presentation.AboutSupportViewModel
import com.dmytrosamoilov.offhand.feature.settings.presentation.NoteStyleEditorViewModel
import com.dmytrosamoilov.offhand.feature.settings.presentation.NoteStylesViewModel
import com.dmytrosamoilov.offhand.feature.settings.presentation.SettingsViewModel
import com.dmytrosamoilov.offhand.feature.settings.presentation.SharedAudioImportViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featureSettingsModule = module {
    factoryOf(::DeleteCustomNoteStyleUseCase)
    factoryOf(::DraftNoteStyleUseCase)
    factoryOf(::GetCustomNoteStyleUseCase)
    factoryOf(::IsCustomNoteStylesAvailableUseCase)
    factoryOf(::ObserveAppLockEnabledUseCase)
    factoryOf(::ObserveCustomNoteStylesUseCase)
    factoryOf(::ObserveDeveloperOptionsUseCase)
    factoryOf(::ObserveDynamicColorUseCase)
    factoryOf(::ObserveNoteFeaturesUseCase)
    factoryOf(::ObserveNoteStyleUseCase)
    factoryOf(::ObserveProOverrideUseCase)
    factoryOf(::ObserveProStatusUseCase)
    factoryOf(::ObserveSmartSuggestionsEnabledUseCase)
    factoryOf(::ObserveTelemetryConsentUseCase)
    factoryOf(::RedeemProCodeUseCase)
    factoryOf(::SaveCustomNoteStyleUseCase)
    factoryOf(::SetAppLockEnabledUseCase)
    factoryOf(::SetDeveloperOptionsUseCase)
    factoryOf(::SetDynamicColorUseCase)
    factoryOf(::SetNoteFeatureEnabledUseCase)
    factoryOf(::SetNoteStyleUseCase)
    factoryOf(::SetProOverrideUseCase)
    factoryOf(::SetSmartSuggestionsEnabledUseCase)
    factoryOf(::SetTelemetryConsentUseCase)
    viewModel {
        SettingsViewModel(
            observeDynamicColor = get(),
            setDynamicColor = get(),
            observeNoteStyle = get(),
            observeCustomNoteStyles = get(),
            observeAppLockEnabled = get(),
            setAppLockEnabled = get(),
            appLockManager = get(),
            observeTelemetryConsent = get(),
            setTelemetryConsent = get(),
            importAudio = get(),
            isAudioImportAvailable = get(),
            observeImportAllowance = get(),
            observeSmartSuggestionsEnabled = get(),
            setSmartSuggestionsEnabled = get(),
            observeNoteFeatures = get(),
            setNoteFeatureEnabled = get(),
            observeProStatus = get(),
            observeProOverride = get(),
            setProOverride = get(),
            proUpgradeGate = get(),
            redeemProCode = get(),
            buildInfo = get(),
            analyticsTracker = get(),
        )
    }
    viewModelOf(::SharedAudioImportViewModel)
    viewModelOf(::AboutSupportViewModel)
    viewModelOf(::NoteStylesViewModel)
    viewModel { parameters ->
        NoteStyleEditorViewModel(
            styleId = parameters.get(),
            getCustomNoteStyle = get(),
            saveCustomNoteStyle = get(),
            deleteCustomNoteStyle = get(),
            draftNoteStyle = get(),
            isCustomNoteStylesAvailable = get(),
            proUpgradeGate = get(),
            analyticsTracker = get(),
        )
    }
}
