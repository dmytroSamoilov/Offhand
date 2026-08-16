package com.dmytrosamoilov.offhand.core.security.di

import com.dmytrosamoilov.offhand.core.security.AndroidAppLockManager
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.core.security.DatabasePassphraseProvider
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.core.security.TinkEncryptedAudioStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreSecurityModule = module {
    singleOf(::AndroidAppLockManager) bind AppLockManager::class
    singleOf(::DatabasePassphraseProvider)
    singleOf(::TinkEncryptedAudioStore) bind EncryptedAudioStore::class
}
