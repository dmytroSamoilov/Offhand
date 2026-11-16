package com.dmytrosamoilov.offhand.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsage
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsageCount
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsageRepository
import kotlinx.coroutines.flow.first

internal class DataStoreProTrialUsageRepository(
    private val dataStore: DataStore<Preferences>,
) : ProTrialUsageRepository {

    override suspend fun add(usage: ProTrialUsage, count: Int) {
        dataStore.edit { it[usage.key()] = (it[usage.key()] ?: 0) + count }
    }

    override suspend fun read(): List<ProTrialUsageCount> {
        val preferences = dataStore.data.first()
        return ProTrialUsage.entries.mapNotNull { usage ->
            preferences[usage.key()]?.takeIf { it > 0 }?.let { ProTrialUsageCount(usage, it) }
        }
    }

    private fun ProTrialUsage.key() = intPreferencesKey("pro_trial_usage_${name.lowercase()}")
}
