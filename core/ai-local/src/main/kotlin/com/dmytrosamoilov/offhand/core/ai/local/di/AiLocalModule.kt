package com.dmytrosamoilov.offhand.core.ai.local.di

import com.dmytrosamoilov.offhand.core.ai.local.ModelCatalog
import com.dmytrosamoilov.offhand.core.ai.local.ModelDownloader
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreAiLocalModule = module {
    singleOf(::ModelCatalog)
    singleOf(::ModelDownloader)
}
