@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.data.domain.SurveyRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.feature.notes.domain.survey.SurveyPromptPolicy
import com.dmytrosamoilov.offhand.feature.notes.domain.survey.UsageSurvey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveUsageSurveyPromptUseCase(
    private val surveyRepository: SurveyRepository,
    private val notesRepository: NotesRepository,
    private val userPreferences: UserPreferencesRepository,
    private val policy: SurveyPromptPolicy,
) {

    operator fun invoke(): Flow<Boolean> = combine(
        surveyRepository.observeState(UsageSurvey.ID),
        notesRepository.observeNotes().map { notes -> notes.filter { it.status == NoteStatus.READY }.map { it.createdAtEpochMs } },
        userPreferences.preferences.map { it.telemetryConsent.isShared },
    ) { state, readyNoteTimesMs, isSharingUsage ->
        policy.shouldAsk(state, readyNoteTimesMs, isSharingUsage, Clock.System.now().toEpochMilliseconds())
    }.distinctUntilChanged()
}
