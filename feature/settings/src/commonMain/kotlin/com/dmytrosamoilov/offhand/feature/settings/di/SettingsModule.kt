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
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveSmartSuggestionsEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.PreviewNoteStyleUseCase
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
    factoryOf(::ObserveNoteStyleUseCase)
    factoryOf(::ObserveProOverrideUseCase)
    factoryOf(::ObserveProStatusUseCase)
    factoryOf(::ObserveSmartSuggestionsEnabledUseCase)
    factoryOf(::ObserveTelemetryConsentUseCase)
    factoryOf(::PreviewNoteStyleUseCase)
    factoryOf(::SaveCustomNoteStyleUseCase)
    factoryOf(::SetAppLockEnabledUseCase)
    factoryOf(::SetDeveloperOptionsUseCase)
    factoryOf(::SetDynamicColorUseCase)
    factoryOf(::SetNoteStyleUseCase)
    factoryOf(::SetProOverrideUseCase)
    factoryOf(::SetSmartSuggestionsEnabledUseCase)
    factoryOf(::SetTelemetryConsentUseCase)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AboutSupportViewModel)
    viewModelOf(::NoteStylesViewModel)
    viewModel { parameters ->
        NoteStyleEditorViewModel(
            styleId = parameters.get(),
            getCustomNoteStyle = get(),
            saveCustomNoteStyle = get(),
            previewNoteStyle = get(),
            draftNoteStyle = get(),
            isCustomNoteStylesAvailable = get(),
            proUpgradeGate = get(),
        )
    }
}
