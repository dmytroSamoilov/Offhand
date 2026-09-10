package com.dmytrosamoilov.offhand.feature.notes.di

import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewPolicy
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewRules
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ClearShareCacheUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.CreateFolderUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.DeleteFolderUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.MoveNoteToFolderUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveCustomNoteStylesUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveFoldersUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.RenameFolderUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.DeleteNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.GetNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.IsCalendarSuggestionsAvailableUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.IsCustomNoteStylesAvailableUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.MarkReviewAttemptUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveNoteSuggestionsUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveNotesUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.SearchNotesUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ShouldRequestReviewUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.UpdateNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.UpdateSuggestionStatusUseCase
import com.dmytrosamoilov.offhand.feature.notes.presentation.NotesViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureNotesModule = module {
    singleOf(::InAppReviewPolicy)
    single { if (get<BuildInfo>().isDebugBuild) InAppReviewRules.DEBUG else InAppReviewRules.PRODUCTION }
    factoryOf(::ClearShareCacheUseCase)
    factoryOf(::CreateFolderUseCase)
    factoryOf(::DeleteFolderUseCase)
    factoryOf(::MoveNoteToFolderUseCase)
    factoryOf(::ObserveCustomNoteStylesUseCase)
    factoryOf(::ObserveFoldersUseCase)
    factoryOf(::RenameFolderUseCase)
    factoryOf(::DeleteNoteUseCase)
    factoryOf(::GetNoteUseCase)
    factoryOf(::IsCalendarSuggestionsAvailableUseCase)
    factoryOf(::IsCustomNoteStylesAvailableUseCase)
    factoryOf(::MarkReviewAttemptUseCase)
    factoryOf(::ObserveDeveloperOptionsUseCase)
    factoryOf(::ObserveNoteSuggestionsUseCase)
    factoryOf(::ObserveNotesUseCase)
    factoryOf(::SearchNotesUseCase)
    factoryOf(::ShouldRequestReviewUseCase)
    factoryOf(::UpdateNoteUseCase)
    factoryOf(::UpdateSuggestionStatusUseCase)
    viewModel {
        NotesViewModel(
            recordingProcessController = get(),
            dateLabelFormatter = get(),
            observeNotes = get(),
            observeFolders = get(),
            searchNotes = get(),
            createFolder = get(),
            renameFolder = get(),
            deleteFolder = get(),
            moveNoteToFolder = get(),
            observeDeveloperOptions = get(),
            observeCustomNoteStyles = get(),
            isCustomNoteStylesAvailable = get(),
            getNote = get(),
            updateNote = get(),
            deleteNote = get(),
            prepareNoteShare = get(),
            clearShareCache = get(),
            shouldRequestReview = get(),
            markReviewAttempt = get(),
            reviewLauncher = get(),
            audioPlayer = get(),
            importAudio = get(),
            isAudioImportAvailable = get(),
            observeNoteSuggestions = get(),
            requestNoteSuggestions = get(),
            updateSuggestionStatus = get(),
            isCalendarSuggestionsAvailable = get(),
            sessionManager = get(),
            aiCoreDownloadStatus = get(),
        )
    }
}
