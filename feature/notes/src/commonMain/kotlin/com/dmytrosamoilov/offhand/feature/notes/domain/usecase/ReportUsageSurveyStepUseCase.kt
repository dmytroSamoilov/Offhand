package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.SurveyRepository
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvent
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.feature.notes.domain.survey.UsageSurvey
import com.dmytrosamoilov.offhand.feature.notes.domain.survey.UsageSurveyStep
import kotlinx.coroutines.flow.first

class ReportUsageSurveyStepUseCase(
    private val surveyRepository: SurveyRepository,
    private val analyticsTracker: AnalyticsTracker,
) {

    suspend operator fun invoke(step: UsageSurveyStep) {
        val attempt = surveyRepository.observeState(UsageSurvey.ID).first().skipCount + 1
        analyticsTracker.track(step.toEvent(attempt))
    }

    private fun UsageSurveyStep.toEvent(attempt: Int): AnalyticsEvent = when (this) {
        UsageSurveyStep.SHOWN -> AnalyticsEvents.surveyShown(UsageSurvey.ID, attempt)
        UsageSurveyStep.ANSWER_CLICKED -> AnalyticsEvents.surveyAnswerClicked(UsageSurvey.ID, attempt)
    }
}
