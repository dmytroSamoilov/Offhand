package com.dmytrosamoilov.offhand.feature.onboarding.di

import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.ObserveUserPreferencesUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetNotePresetUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.presentation.OnboardingViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featureOnboardingModule = module {
    factoryOf(::CompleteOnboardingUseCase)
    factoryOf(::ObserveUserPreferencesUseCase)
    factoryOf(::SetAppLockEnabledUseCase)
    factoryOf(::SetNotePresetUseCase)
    factoryOf(::SetTelemetryConsentUseCase)
    viewModelOf(::OnboardingViewModel)
}
