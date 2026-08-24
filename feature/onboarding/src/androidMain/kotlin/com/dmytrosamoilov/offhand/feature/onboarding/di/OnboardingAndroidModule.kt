package com.dmytrosamoilov.offhand.feature.onboarding.di

import com.dmytrosamoilov.offhand.core.common.ModelDownloadController
import com.dmytrosamoilov.offhand.feature.onboarding.presentation.OnboardingStepPolicy
import com.dmytrosamoilov.offhand.feature.onboarding.service.ModelDownloadControllerImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val featureOnboardingAndroidModule = module {
    singleOf(::ModelDownloadControllerImpl) bind ModelDownloadController::class
    // Android asks for POST_NOTIFICATIONS with the system dialog on the
    // download step, so the dedicated explainer page stays iOS-only.
    single { OnboardingStepPolicy(asksNotificationPermission = false) }
}
