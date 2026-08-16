package com.dmytrosamoilov.offhand.feature.recording.di

import com.dmytrosamoilov.offhand.feature.recording.domain.DefaultNoteTitleProvider
import com.dmytrosamoilov.offhand.feature.recording.presentation.DefaultNoteTitleProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NoteTitleModule {

    @Binds
    abstract fun bindDefaultNoteTitleProvider(
        implementation: DefaultNoteTitleProviderImpl,
    ): DefaultNoteTitleProvider
}
