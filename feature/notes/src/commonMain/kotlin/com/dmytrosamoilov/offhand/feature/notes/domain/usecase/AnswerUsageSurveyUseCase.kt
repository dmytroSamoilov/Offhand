package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.SurveyRepository
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.feature.notes.domain.survey.UsageSurvey
import com.dmytrosamoilov.offhand.feature.notes.domain.survey.UsageSurveyAnswer

class AnswerUsageSurveyUseCase(
    private val surveyRepository: SurveyRepository,
    private val analyticsTracker: AnalyticsTracker,
) {

    // Stored first: an answer that was sent but not remembered would be asked
    // for again. Several choices travel as one list in a fixed order, so the
    // same combination always reads the same.
    suspend operator fun invoke(answers: Set<UsageSurveyAnswer>) {
        if (answers.isEmpty()) return
        val answer = UsageSurveyAnswer.entries.filter { it in answers }.joinToString(ANSWER_SEPARATOR) { it.analyticsName }
        surveyRepository.saveAnswer(UsageSurvey.ID, answer)
        analyticsTracker.track(AnalyticsEvents.surveyAnswered(UsageSurvey.ID, answer))
    }

    private companion object {
        const val ANSWER_SEPARATOR = ","
    }
}
