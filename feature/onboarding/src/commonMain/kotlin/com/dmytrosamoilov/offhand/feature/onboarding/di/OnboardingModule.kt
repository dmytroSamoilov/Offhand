package com.dmytrosamoilov.offhand.feature.onboarding.di

import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.AnswerTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.GetTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.ObserveUserPreferencesUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.presentation.OnboardingViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featureOnboardingModule = module {
    factoryOf(::AnswerTelemetryConsentUseCase)
    factoryOf(::CompleteOnboardingUseCase)
    factoryOf(::GetTelemetryConsentUseCase)
    factoryOf(::ObserveUserPreferencesUseCase)
    factoryOf(::SetAppLockEnabledUseCase)
    viewModelOf(::OnboardingViewModel)
}
