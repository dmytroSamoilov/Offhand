package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.Flow

// What the install has done with one in-app question. Every question has its
// own id, so a later one starts clean whatever was answered before.
data class SurveyState(
    val answer: String? = null,
    val skippedAtMs: Long? = null,
    val skipCount: Int = 0,
) {
    val isAnswered: Boolean get() = answer != null
}

interface SurveyRepository {

    fun observeState(surveyId: String): Flow<SurveyState>

    suspend fun saveAnswer(surveyId: String, answer: String)

    suspend fun saveSkip(surveyId: String, atMs: Long)
}
