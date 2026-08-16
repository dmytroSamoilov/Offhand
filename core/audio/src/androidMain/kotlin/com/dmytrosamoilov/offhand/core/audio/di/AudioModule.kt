package com.dmytrosamoilov.offhand.core.audio.di

import com.dmytrosamoilov.offhand.core.audio.PcmAudioPlayer
import com.dmytrosamoilov.offhand.core.audio.StreamingAudioRecorder
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreAudioModule = module {
    singleOf(::StreamingAudioRecorder)
    factoryOf(::PcmAudioPlayer)
}
