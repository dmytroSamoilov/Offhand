package com.dmytrosamoilov.offhand.feature.recording.di

import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.feature.recording.domain.IosRecordingAudioBackup
import com.dmytrosamoilov.offhand.feature.recording.domain.IosRecordingProcessController
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingAudioBackup
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val featureRecordingIosModule = module {
    singleOf(::IosRecordingAudioBackup) bind RecordingAudioBackup::class
    singleOf(::IosRecordingProcessController) bind RecordingProcessController::class
}
