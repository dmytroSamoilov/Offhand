package com.dmytrosamoilov.offhand.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dmytrosamoilov.offhand.core.data.domain.SurveyRepository
import com.dmytrosamoilov.offhand.core.data.domain.SurveyState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// The keys carry the question's id and nothing else, so an app update never
// asks again and a new question never reads an old answer.
internal class DataStoreSurveyRepository(
    private val dataStore: DataStore<Preferences>,
) : SurveyRepository {

    override fun observeState(surveyId: String): Flow<SurveyState> =
        dataStore.data.map { preferences ->
            SurveyState(
                answer = preferences[answerKey(surveyId)],
                skippedAtMs = preferences[skippedAtKey(surveyId)],
                skipCount = preferences[skipCountKey(surveyId)] ?: 0,
            )
        }.distinctUntilChanged()

    override suspend fun saveAnswer(surveyId: String, answer: String) {
        dataStore.edit { it[answerKey(surveyId)] = answer }
    }

    override suspend fun saveSkip(surveyId: String, atMs: Long) {
        dataStore.edit { preferences ->
            preferences[skippedAtKey(surveyId)] = atMs
            preferences[skipCountKey(surveyId)] = (preferences[skipCountKey(surveyId)] ?: 0) + 1
        }
    }

    private fun answerKey(surveyId: String) = stringPreferencesKey("survey_${surveyId}_answer")

    private fun skippedAtKey(surveyId: String) = longPreferencesKey("survey_${surveyId}_skipped_at")

    private fun skipCountKey(surveyId: String) = intPreferencesKey("survey_${surveyId}_skip_count")
}
