package com.dmytrosamoilov.offhand.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusCache
import kotlinx.coroutines.flow.first

internal class DataStoreProStatusCache(
    private val dataStore: DataStore<Preferences>,
) : ProStatusCache {

    override suspend fun read(): ProStatus {
        val preferences = dataStore.data.first()
        val plan = preferences[KEY_PLAN]?.let { name -> ProPlan.entries.firstOrNull { it.name == name } }
            ?: return ProStatus.FREE
        return ProStatus(
            plan = plan,
            isTrial = preferences[KEY_TRIAL] ?: false,
            renewsAtMs = preferences[KEY_RENEWS_AT_MS],
        )
    }

    override suspend fun write(status: ProStatus) {
        dataStore.edit { preferences ->
            preferences[KEY_PLAN] = status.plan.name
            preferences[KEY_TRIAL] = status.isTrial
            val renewsAt = status.renewsAtMs
            if (renewsAt == null) preferences.remove(KEY_RENEWS_AT_MS) else preferences[KEY_RENEWS_AT_MS] = renewsAt
        }
    }

    private companion object {
        val KEY_PLAN = stringPreferencesKey("pro_plan")
        val KEY_TRIAL = booleanPreferencesKey("pro_trial")
        val KEY_RENEWS_AT_MS = longPreferencesKey("pro_renews_at_ms")
    }
}
