package com.dmytrosamoilov.offhand.feature.recording.di

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleDrafter
import com.dmytrosamoilov.offhand.feature.recording.domain.CalendarEventExtractor
import com.dmytrosamoilov.offhand.feature.recording.domain.NoteStyleResolver
import com.dmytrosamoilov.offhand.feature.recording.domain.PendingNotesCoordinator
import com.dmytrosamoilov.offhand.feature.recording.domain.SessionNoteStyleDrafter
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.domain.TranscriptStructurer
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ClearTranscriptionCheckpointUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CompleteNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CreateImportedNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CreateRecordingNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.DiscardNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.FailNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.GetNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.GetNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.GetTranscriptionCheckpointUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ImportAudioUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAudioImportAvailableUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAiCoreDownloadedUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsCalendarSuggestionsAvailableUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsThinkingEnabledUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.MarkNoteProcessingUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.MarkNoteRecordedUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.RegisterSavedRecordingUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.RequestNoteSuggestionsUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ResumeInterruptedNotesUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SaveNoteSuggestionsUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SaveNoteTranscriptUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SaveTranscriptionCheckpointUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SweepOrphanedRecordingsUseCase
import com.dmytrosamoilov.offhand.feature.recording.presentation.RecordingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val recordingSessionScopeQualifier = named("recordingSessionScope")

val featureRecordingModule = module {
    single(recordingSessionScopeQualifier) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    singleOf(::NoteStyleResolver)
    singleOf(::TranscriptStructurer)
    singleOf(::SessionNoteStyleDrafter) bind NoteStyleDrafter::class
    singleOf(::CalendarEventExtractor)
    singleOf(::PendingNotesCoordinator)
    singleOf(::ResumeInterruptedNotesUseCase)
    single {
        RecordingSessionManager(
            recorder = get(),
            speechToText = get(),
            transcriptStructurer = get(),
            createRecordingNote = get(),
            createImportedNote = get(),
            markNoteRecorded = get(),
            discardNote = get(),
            completeNote = get(),
            failNote = get(),
            markNoteProcessing = get(),
            registerSavedRecording = get(),
            saveNoteTranscript = get(),
            getTranscriptionCheckpoint = get(),
            saveTranscriptionCheckpoint = get(),
            clearTranscriptionCheckpoint = get(),
            isAiCoreDownloaded = get(),
            getNoteStyle = get(),
            getNote = get(),
            calendarEventExtractor = get(),
            saveNoteSuggestions = get(),
            isCalendarSuggestionsAvailable = get(),
            audioStore = get(),
            audioBackup = get(),
            audioDecoder = get(),
            scope = get(recordingSessionScopeQualifier),
        )
    }
    factoryOf(::ClearTranscriptionCheckpointUseCase)
    factoryOf(::CompleteNoteUseCase)
    factoryOf(::CreateImportedNoteUseCase)
    factoryOf(::CreateRecordingNoteUseCase)
    factoryOf(::DiscardNoteUseCase)
    factoryOf(::FailNoteUseCase)
    factoryOf(::GetNoteStyleUseCase)
    factoryOf(::GetNoteUseCase)
    factoryOf(::GetTranscriptionCheckpointUseCase)
    factoryOf(::ImportAudioUseCase)
    factoryOf(::IsAudioImportAvailableUseCase)
    factoryOf(::IsAiCoreDownloadedUseCase)
    factoryOf(::IsCalendarSuggestionsAvailableUseCase)
    factoryOf(::IsThinkingEnabledUseCase)
    factoryOf(::MarkNoteProcessingUseCase)
    factoryOf(::MarkNoteRecordedUseCase)
    factoryOf(::ObserveDeveloperOptionsUseCase)
    factoryOf(::RegisterSavedRecordingUseCase)
    factoryOf(::RequestNoteSuggestionsUseCase)
    factoryOf(::SaveNoteSuggestionsUseCase)
    factoryOf(::SaveNoteTranscriptUseCase)
    factoryOf(::SaveTranscriptionCheckpointUseCase)
    factoryOf(::SweepOrphanedRecordingsUseCase)
    viewModelOf(::RecordingViewModel)
}
