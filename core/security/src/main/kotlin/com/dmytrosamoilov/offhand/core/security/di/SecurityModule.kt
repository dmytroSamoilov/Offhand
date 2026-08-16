package com.dmytrosamoilov.offhand.core.security.di

import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.core.security.DatabasePassphraseProvider
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreSecurityModule = module {
    singleOf(::AppLockManager)
    singleOf(::DatabasePassphraseProvider)
    singleOf(::EncryptedAudioStore)
}
