package com.dmytrosamoilov.offhand.feature.notes.di

import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.feature.notes.domain.DateLabelFormatter
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabelsProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.ShareCacheDirectoryProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.review.AppInstallInfoProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewPolicy
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewRules
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.DeleteNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.GetNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.MarkReviewAttemptUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveNotesUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.PrepareNoteShareUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ShouldRequestReviewUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.UpdateNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.presentation.DateLabelFormatterImpl
import com.dmytrosamoilov.offhand.feature.notes.presentation.FakeInAppReviewLauncher
import com.dmytrosamoilov.offhand.feature.notes.presentation.InAppReviewLauncher
import com.dmytrosamoilov.offhand.feature.notes.presentation.NoteShareLabelsProviderImpl
import com.dmytrosamoilov.offhand.feature.notes.presentation.NotesViewModel
import com.dmytrosamoilov.offhand.feature.notes.presentation.PlayInAppReviewLauncher
import com.dmytrosamoilov.offhand.feature.notes.presentation.ShareCacheDirectoryProviderImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val featureNotesModule = module {
    singleOf(::DateLabelFormatterImpl) bind DateLabelFormatter::class
    singleOf(::AppInstallInfoProvider)
    singleOf(::InAppReviewPolicy)
    single { if (get<BuildInfo>().isDebugBuild) InAppReviewRules.DEBUG else InAppReviewRules.PRODUCTION }
    singleOf(::PlayInAppReviewLauncher)
    singleOf(::FakeInAppReviewLauncher)
    single<InAppReviewLauncher> {
        if (get<BuildInfo>().isDebugBuild) get<FakeInAppReviewLauncher>() else get<PlayInAppReviewLauncher>()
    }
    singleOf(::NoteShareLabelsProviderImpl) bind NoteShareLabelsProvider::class
    singleOf(::ShareCacheDirectoryProviderImpl) bind ShareCacheDirectoryProvider::class
    factoryOf(::DeleteNoteUseCase)
    factoryOf(::GetNoteUseCase)
    factoryOf(::MarkReviewAttemptUseCase)
    factoryOf(::ObserveDeveloperOptionsUseCase)
    factoryOf(::ObserveNotesUseCase)
    factoryOf(::PrepareNoteShareUseCase)
    factoryOf(::ShouldRequestReviewUseCase)
    factoryOf(::UpdateNoteUseCase)
    viewModelOf(::NotesViewModel)
}
