@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.notes.di

import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewPolicy
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewRules
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ClearShareCacheUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ClearTranscriptionCheckpointUseCase
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
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.IsDocumentExportAvailableUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.IsSmartSuggestionsEnabledUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.export.NoteDocumentBuilder
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.MarkReviewAttemptUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveNoteSuggestionsUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveNotesUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.SearchNotesUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ShouldRequestReviewUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.UpdateNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.UpdateSuggestionStatusUseCase
import com.dmytrosamoilov.offhand.feature.notes.presentation.NotesViewModel
import kotlin.time.ExperimentalTime
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureNotesModule = module {
    singleOf(::InAppReviewPolicy)
    single { if (get<BuildInfo>().isDeveloperBuild) InAppReviewRules.DEBUG else InAppReviewRules.PRODUCTION }
    factoryOf(::ClearShareCacheUseCase)
    factoryOf(::ClearTranscriptionCheckpointUseCase)
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
    factoryOf(::IsDocumentExportAvailableUseCase)
    factoryOf(::IsSmartSuggestionsEnabledUseCase)
    factory { NoteDocumentBuilder(get(), get(), get()) }
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
            observeNoteSuggestions = get(),
            requestNoteSuggestions = get(),
            updateSuggestionStatus = get(),
            isCalendarSuggestionsAvailable = get(),
            isDocumentExportAvailable = get(),
            isSmartSuggestionsEnabled = get(),
            proUpgradeGate = get(),
            sessionManager = get(),
            aiCoreDownloadStatus = get(),
            clearTranscriptionCheckpoint = get(),
            buildInfo = get(),
        )
    }
}
