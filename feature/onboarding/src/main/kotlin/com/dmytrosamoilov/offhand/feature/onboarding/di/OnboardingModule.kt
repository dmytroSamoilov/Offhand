package com.dmytrosamoilov.offhand.feature.onboarding.di

import com.dmytrosamoilov.offhand.core.common.ModelDownloadController
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.ObserveUserPreferencesUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetNotePresetUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.presentation.OnboardingViewModel
import com.dmytrosamoilov.offhand.feature.onboarding.service.ModelDownloadControllerImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val featureOnboardingModule = module {
    singleOf(::ModelDownloadControllerImpl) bind ModelDownloadController::class
    factoryOf(::CompleteOnboardingUseCase)
    factoryOf(::ObserveUserPreferencesUseCase)
    factoryOf(::SetNotePresetUseCase)
    factoryOf(::SetTelemetryConsentUseCase)
    viewModelOf(::OnboardingViewModel)
}
