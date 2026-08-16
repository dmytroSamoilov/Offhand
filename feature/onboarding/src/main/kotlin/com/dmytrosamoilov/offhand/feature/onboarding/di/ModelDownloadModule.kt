package com.dmytrosamoilov.offhand.feature.onboarding.di

import com.dmytrosamoilov.offhand.core.common.ModelDownloadController
import com.dmytrosamoilov.offhand.feature.onboarding.service.ModelDownloadControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ModelDownloadModule {

    @Binds
    abstract fun bindModelDownloadController(
        implementation: ModelDownloadControllerImpl,
    ): ModelDownloadController
}
