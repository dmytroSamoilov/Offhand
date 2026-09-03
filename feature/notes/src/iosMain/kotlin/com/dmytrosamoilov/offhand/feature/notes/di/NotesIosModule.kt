package com.dmytrosamoilov.offhand.feature.notes.di

import com.dmytrosamoilov.offhand.feature.notes.domain.AudioPlayer
import com.dmytrosamoilov.offhand.feature.notes.domain.DateLabelFormatter
import com.dmytrosamoilov.offhand.feature.notes.domain.IosAudioPlayer
import com.dmytrosamoilov.offhand.feature.notes.domain.ShareCacheDirectoryProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.review.AppInstallInfoProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.IosPrepareNoteShareUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.PrepareNoteShareUseCase
import com.dmytrosamoilov.offhand.feature.notes.presentation.InAppReviewLauncher
import com.dmytrosamoilov.offhand.feature.notes.presentation.IosAppInstallInfoProvider
import com.dmytrosamoilov.offhand.feature.notes.presentation.IosDateLabelFormatter
import com.dmytrosamoilov.offhand.feature.notes.presentation.IosShareCacheDirectoryProvider
import com.dmytrosamoilov.offhand.feature.notes.presentation.NoOpInAppReviewLauncher
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val featureNotesIosModule = module {
    factoryOf(::IosAudioPlayer) bind AudioPlayer::class
    singleOf(::IosDateLabelFormatter) bind DateLabelFormatter::class
    singleOf(::IosShareCacheDirectoryProvider) bind ShareCacheDirectoryProvider::class
    singleOf(::IosAppInstallInfoProvider) bind AppInstallInfoProvider::class
    single<InAppReviewLauncher> { NoOpInAppReviewLauncher }
    singleOf(::IosPrepareNoteShareUseCase) bind PrepareNoteShareUseCase::class
}
