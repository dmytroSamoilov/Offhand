package com.dmytrosamoilov.offhand.core.security.di

import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.core.security.IosAppLockManager
import com.dmytrosamoilov.offhand.core.security.IosEncryptedAudioStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreSecurityModule = module {
    singleOf(::IosAppLockManager) bind AppLockManager::class
    singleOf(::IosEncryptedAudioStore) bind EncryptedAudioStore::class
}
