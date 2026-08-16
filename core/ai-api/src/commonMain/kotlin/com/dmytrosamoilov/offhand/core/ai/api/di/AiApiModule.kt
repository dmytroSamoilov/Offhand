package com.dmytrosamoilov.offhand.core.ai.api.di

import com.dmytrosamoilov.offhand.core.ai.api.AiCoreDownloadStatus
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreAiApiModule = module {
    singleOf(::AiCoreDownloadStatus)
}
