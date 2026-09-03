package com.dmytrosamoilov.offhand.feature.recording.di

import com.dmytrosamoilov.offhand.feature.recording.domain.PendingNotesCoordinator
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.domain.TranscriptStructurer
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CompleteNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CreateRecordingNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.DiscardNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.FailNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.GetNotePresetUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.GetNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAiCoreDownloadedUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsThinkingEnabledUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.MarkNoteProcessingUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.MarkNoteRecordedUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.RegisterSavedRecordingUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ResumeInterruptedNotesUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SaveNoteTranscriptUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SweepOrphanedRecordingsUseCase
import com.dmytrosamoilov.offhand.feature.recording.presentation.RecordingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val recordingSessionScopeQualifier = named("recordingSessionScope")

val featureRecordingModule = module {
    single(recordingSessionScopeQualifier) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    singleOf(::TranscriptStructurer)
    singleOf(::PendingNotesCoordinator)
    singleOf(::ResumeInterruptedNotesUseCase)
    single {
        RecordingSessionManager(
            recorder = get(),
            speechToText = get(),
            transcriptStructurer = get(),
            createRecordingNote = get(),
            markNoteRecorded = get(),
            discardNote = get(),
            completeNote = get(),
            failNote = get(),
            markNoteProcessing = get(),
            registerSavedRecording = get(),
            saveNoteTranscript = get(),
            isAiCoreDownloaded = get(),
            getNotePreset = get(),
            getNote = get(),
            audioStore = get(),
            audioBackup = get(),
            scope = get(recordingSessionScopeQualifier),
        )
    }
    factoryOf(::CompleteNoteUseCase)
    factoryOf(::CreateRecordingNoteUseCase)
    factoryOf(::DiscardNoteUseCase)
    factoryOf(::FailNoteUseCase)
    factoryOf(::GetNotePresetUseCase)
    factoryOf(::GetNoteUseCase)
    factoryOf(::IsAiCoreDownloadedUseCase)
    factoryOf(::IsThinkingEnabledUseCase)
    factoryOf(::MarkNoteProcessingUseCase)
    factoryOf(::MarkNoteRecordedUseCase)
    factoryOf(::ObserveDeveloperOptionsUseCase)
    factoryOf(::RegisterSavedRecordingUseCase)
    factoryOf(::SaveNoteTranscriptUseCase)
    factoryOf(::SweepOrphanedRecordingsUseCase)
    viewModelOf(::RecordingViewModel)
}
