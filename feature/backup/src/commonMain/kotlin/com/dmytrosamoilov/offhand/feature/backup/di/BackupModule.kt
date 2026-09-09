package com.dmytrosamoilov.offhand.feature.backup.di

import com.dmytrosamoilov.offhand.feature.backup.domain.usecase.CreateBackupUseCase
import com.dmytrosamoilov.offhand.feature.backup.domain.usecase.RestoreBackupUseCase
import com.dmytrosamoilov.offhand.feature.backup.presentation.BackupViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featureBackupModule = module {
    factoryOf(::CreateBackupUseCase)
    factoryOf(::RestoreBackupUseCase)
    viewModelOf(::BackupViewModel)
}
