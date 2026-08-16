package com.dmytrosamoilov.offhand.feature.recording.di

import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.feature.recording.domain.AndroidAudioRecorder
import com.dmytrosamoilov.offhand.feature.recording.domain.AndroidRecordingAudioBackup
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioRecorder
import com.dmytrosamoilov.offhand.feature.recording.domain.DefaultNoteTitleProvider
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingAudioBackup
import com.dmytrosamoilov.offhand.feature.recording.presentation.DefaultNoteTitleProviderImpl
import com.dmytrosamoilov.offhand.feature.recording.service.RecordingProcessControllerImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val featureRecordingAndroidModule = module {
    singleOf(::DefaultNoteTitleProviderImpl) bind DefaultNoteTitleProvider::class
    singleOf(::RecordingProcessControllerImpl) bind RecordingProcessController::class
    singleOf(::AndroidAudioRecorder) bind AudioRecorder::class
    singleOf(::AndroidRecordingAudioBackup) bind RecordingAudioBackup::class
}
