@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.SurveyRepository
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.feature.notes.domain.survey.UsageSurvey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.first

class SkipUsageSurveyUseCase(
    private val surveyRepository: SurveyRepository,
    private val analyticsTracker: AnalyticsTracker,
) {

    suspend operator fun invoke() {
        val attempt = surveyRepository.observeState(UsageSurvey.ID).first().skipCount + 1
        surveyRepository.saveSkip(UsageSurvey.ID, Clock.System.now().toEpochMilliseconds())
        analyticsTracker.track(AnalyticsEvents.surveySkipped(UsageSurvey.ID, attempt))
    }
}
