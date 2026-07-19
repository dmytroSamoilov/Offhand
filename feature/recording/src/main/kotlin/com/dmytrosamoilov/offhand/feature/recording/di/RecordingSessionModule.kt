package com.dmytrosamoilov.offhand.feature.recording.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RecordingSessionScope

@Module
@InstallIn(SingletonComponent::class)
internal object RecordingSessionModule {

    @Provides
    @Singleton
    @RecordingSessionScope
    fun provideRecordingSessionScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
