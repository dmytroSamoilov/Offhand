package com.dmytrosamoilov.offhand.feature.notes.di

import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewPolicy
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewRules
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ClearShareCacheUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.CreateFolderUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.DeleteFolderUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.MoveNoteToFolderUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveFoldersUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.RenameFolderUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.DeleteNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.GetNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.MarkReviewAttemptUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveNotesUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.SearchNotesUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ShouldRequestReviewUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.UpdateNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.presentation.NotesViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featureNotesModule = module {
    singleOf(::InAppReviewPolicy)
    single { if (get<BuildInfo>().isDebugBuild) InAppReviewRules.DEBUG else InAppReviewRules.PRODUCTION }
    factoryOf(::ClearShareCacheUseCase)
    factoryOf(::CreateFolderUseCase)
    factoryOf(::DeleteFolderUseCase)
    factoryOf(::MoveNoteToFolderUseCase)
    factoryOf(::ObserveFoldersUseCase)
    factoryOf(::RenameFolderUseCase)
    factoryOf(::DeleteNoteUseCase)
    factoryOf(::GetNoteUseCase)
    factoryOf(::MarkReviewAttemptUseCase)
    factoryOf(::ObserveDeveloperOptionsUseCase)
    factoryOf(::ObserveNotesUseCase)
    factoryOf(::SearchNotesUseCase)
    factoryOf(::ShouldRequestReviewUseCase)
    factoryOf(::UpdateNoteUseCase)
    viewModelOf(::NotesViewModel)
}
