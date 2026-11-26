package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.SurveyRepository
import com.dmytrosamoilov.offhand.core.data.domain.SurveyState
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvent
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.feature.notes.domain.survey.UsageSurvey
import com.dmytrosamoilov.offhand.feature.notes.domain.survey.UsageSurveyAnswer
import com.dmytrosamoilov.offhand.feature.notes.domain.survey.UsageSurveyStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest

class UsageSurveyUseCasesTest {

    private val repository = InMemorySurveyRepository()
    private val tracker = RecordingTracker()

    @Test
    fun `an answer is stored under the question id and sent once`() = runTest {
        AnswerUsageSurveyUseCase(repository, tracker)(setOf(UsageSurveyAnswer.THOUGHTS))

        assertEquals("thoughts", repository.stateOf(UsageSurvey.ID).answer)
        assertEquals(
            listOf(AnalyticsEvent("survey_answered", mapOf("question" to "mostly_record_v1", "answer" to "thoughts"))),
            tracker.events,
        )
    }

    @Test
    fun `several choices are stored and sent as one list in a fixed order`() = runTest {
        AnswerUsageSurveyUseCase(repository, tracker)(
            setOf(UsageSurveyAnswer.OTHER, UsageSurveyAnswer.MEETINGS, UsageSurveyAnswer.LECTURES),
        )

        assertEquals("meetings,lectures,other", repository.stateOf(UsageSurvey.ID).answer)
        assertEquals(listOf<Any?>("meetings,lectures,other"), tracker.events.map { it.params["answer"] })
    }

    @Test
    fun `an empty selection stores and sends nothing`() = runTest {
        AnswerUsageSurveyUseCase(repository, tracker)(emptySet())

        assertEquals(SurveyState(), repository.stateOf(UsageSurvey.ID))
        assertEquals(emptyList(), tracker.events)
    }

    @Test
    fun `every answer has its own analytics value`() {
        assertEquals(
            listOf("meetings", "thoughts", "lectures", "interviews", "other"),
            UsageSurveyAnswer.entries.map { it.analyticsName },
        )
    }

    @Test
    fun `each not now is counted and reported with its attempt`() = runTest {
        val skip = SkipUsageSurveyUseCase(repository, tracker)

        skip()
        skip()

        val state = repository.stateOf(UsageSurvey.ID)
        assertEquals(2, state.skipCount)
        assertNotNull(state.skippedAtMs)
        assertEquals(listOf(1L, 2L), tracker.events.map { it.params["attempt"] })
        assertEquals(setOf("survey_skipped"), tracker.events.map { it.name }.toSet())
    }

    @Test
    fun `showing the question and tapping answer are reported with the attempt and store nothing`() = runTest {
        val report = ReportUsageSurveyStepUseCase(repository, tracker)

        report(UsageSurveyStep.SHOWN)
        report(UsageSurveyStep.ANSWER_CLICKED)
        SkipUsageSurveyUseCase(repository, tracker)()
        report(UsageSurveyStep.SHOWN)

        assertEquals(
            listOf("survey_shown" to 1L, "survey_answer_clicked" to 1L, "survey_skipped" to 1L, "survey_shown" to 2L),
            tracker.events.map { it.name to it.params["attempt"] },
        )
        assertEquals(null, repository.stateOf(UsageSurvey.ID).answer)
    }

    @Test
    fun `another question keeps its own state`() = runTest {
        AnswerUsageSurveyUseCase(repository, tracker)(setOf(UsageSurveyAnswer.OTHER))

        assertEquals(SurveyState(), repository.stateOf("another_question_v1"))
    }
}

private class InMemorySurveyRepository : SurveyRepository {

    private val states = MutableStateFlow<Map<String, SurveyState>>(emptyMap())

    fun stateOf(surveyId: String): SurveyState = states.value[surveyId] ?: SurveyState()

    override fun observeState(surveyId: String): Flow<SurveyState> = states.map { it[surveyId] ?: SurveyState() }

    override suspend fun saveAnswer(surveyId: String, answer: String) {
        states.update { it + (surveyId to stateOf(surveyId).copy(answer = answer)) }
    }

    override suspend fun saveSkip(surveyId: String, atMs: Long) {
        val state = stateOf(surveyId)
        states.update { it + (surveyId to state.copy(skippedAtMs = atMs, skipCount = state.skipCount + 1)) }
    }
}

private class RecordingTracker : AnalyticsTracker {

    val events = mutableListOf<AnalyticsEvent>()

    override fun track(event: AnalyticsEvent) {
        events += event
    }
}
