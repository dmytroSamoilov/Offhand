package com.dmytrosamoilov.offhand.feature.notes.di

import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.feature.notes.domain.AndroidAudioPlayer
import com.dmytrosamoilov.offhand.feature.notes.domain.AudioPlayer
import com.dmytrosamoilov.offhand.feature.notes.domain.DateLabelFormatter
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabelsProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.ShareCacheDirectoryProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.review.AndroidAppInstallInfoProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.review.AppInstallInfoProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.AndroidPrepareNoteShareUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.PrepareNoteShareUseCase
import com.dmytrosamoilov.offhand.feature.notes.presentation.DateLabelFormatterImpl
import com.dmytrosamoilov.offhand.feature.notes.presentation.FakeInAppReviewLauncher
import com.dmytrosamoilov.offhand.feature.notes.presentation.InAppReviewLauncher
import com.dmytrosamoilov.offhand.feature.notes.presentation.NoteShareLabelsProviderImpl
import com.dmytrosamoilov.offhand.feature.notes.presentation.PlayInAppReviewLauncher
import com.dmytrosamoilov.offhand.feature.notes.presentation.ShareCacheDirectoryProviderImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val featureNotesAndroidModule = module {
    singleOf(::DateLabelFormatterImpl) bind DateLabelFormatter::class
    singleOf(::AndroidAppInstallInfoProvider) bind AppInstallInfoProvider::class
    singleOf(::PlayInAppReviewLauncher)
    singleOf(::FakeInAppReviewLauncher)
    single<InAppReviewLauncher> {
        if (get<BuildInfo>().isDebugBuild) get<FakeInAppReviewLauncher>() else get<PlayInAppReviewLauncher>()
    }
    singleOf(::NoteShareLabelsProviderImpl) bind NoteShareLabelsProvider::class
    singleOf(::ShareCacheDirectoryProviderImpl) bind ShareCacheDirectoryProvider::class
    factoryOf(::AndroidPrepareNoteShareUseCase) bind PrepareNoteShareUseCase::class
    factoryOf(::AndroidAudioPlayer) bind AudioPlayer::class
}
