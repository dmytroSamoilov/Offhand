package com.dmytrosamoilov.offhand.feature.recording.di

import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.feature.recording.service.RecordingProcessControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RecordingProcessModule {

    @Binds
    abstract fun bindRecordingProcessController(
        implementation: RecordingProcessControllerImpl,
    ): RecordingProcessController
}
