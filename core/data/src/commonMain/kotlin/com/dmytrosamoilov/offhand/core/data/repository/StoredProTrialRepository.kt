package com.dmytrosamoilov.offhand.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialNotice
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialPolicy
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRecord
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRecordStore
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialState
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

// The record lives twice: in the app's own preferences, which answer at once
// and offline, and in the reinstall-proof store. The latest time ever seen is
// kept next to it, so setting the clock back never buys trial days.
internal class StoredProTrialRepository(
    private val dataStore: DataStore<Preferences>,
    private val reinstallProofStore: ProTrialRecordStore,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ProTrialRepository {

    override fun observeState(): Flow<ProTrialState> = dataStore.data
        .map { preferences -> preferences.record() to (preferences[KEY_LAST_SEEN_AT_MS] ?: 0L) }
        .distinctUntilChanged()
        .transformLatest { (record, lastSeenAtMs) ->
            val now = maxOf(nowMs(), lastSeenAtMs)
            val state = ProTrialPolicy.stateOf(record, now)
            emit(state)
            if (state is ProTrialState.Active) {
                delay(state.endsAtMs - now)
                emit(ProTrialState.Ended)
            }
        }
        .distinctUntilChanged()

    override suspend fun currentState(): ProTrialState = observeState().first()

    override suspend fun readRecord(): ProTrialRecord? {
        val local = dataStore.data.first().record()
        val merged = ProTrialPolicy.merge(local, readReinstallProof())
        if (merged != null && merged != local) writeLocal(merged)
        return merged
    }

    override suspend fun writeRecord(record: ProTrialRecord) {
        writeLocal(record)
        writeReinstallProof(record)
    }

    override suspend fun rememberCurrentTime() {
        val now = nowMs()
        dataStore.edit { it[KEY_LAST_SEEN_AT_MS] = maxOf(now, it[KEY_LAST_SEEN_AT_MS] ?: 0L) }
    }

    override suspend fun isNoticeSeen(notice: ProTrialNotice): Boolean =
        dataStore.data.first()[notice.key()] ?: false

    override suspend fun markNoticeSeen(notice: ProTrialNotice) {
        dataStore.edit { it[notice.key()] = true }
    }

    override suspend fun keepInSync() {
        reinstallProofStore.externalChanges.collect {
            val local = dataStore.data.first().record()
            val remote = readReinstallProof()
            val merged = ProTrialPolicy.merge(local, remote) ?: return@collect
            if (merged != local) writeLocal(merged)
            if (merged != remote) writeReinstallProof(merged)
        }
    }

    private suspend fun writeLocal(record: ProTrialRecord) {
        val now = nowMs()
        dataStore.edit { preferences ->
            preferences[KEY_STARTED_AT_MS] = record.startedAtMs
            preferences[KEY_REVOKED] = record.isRevoked
            preferences[KEY_LAST_SEEN_AT_MS] = maxOf(now, preferences[KEY_LAST_SEEN_AT_MS] ?: 0L)
        }
    }

    private suspend fun readReinstallProof(): ProTrialRecord? = try {
        reinstallProofStore.read()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend fun writeReinstallProof(record: ProTrialRecord) {
        try {
            reinstallProofStore.write(record)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return
        }
    }

    private fun Preferences.record(): ProTrialRecord? {
        val startedAtMs = this[KEY_STARTED_AT_MS] ?: return null
        return ProTrialRecord(startedAtMs = startedAtMs, isRevoked = this[KEY_REVOKED] ?: false)
    }

    private fun ProTrialNotice.key(): Preferences.Key<Boolean> = when (this) {
        ProTrialNotice.ENDED -> KEY_ENDED_SEEN
        ProTrialNotice.REMOVED -> KEY_REMOVED_SEEN
    }

    private companion object {
        val KEY_STARTED_AT_MS = longPreferencesKey("pro_trial_started_at_ms")
        val KEY_REVOKED = booleanPreferencesKey("pro_trial_revoked")
        val KEY_LAST_SEEN_AT_MS = longPreferencesKey("pro_trial_last_seen_at_ms")
        val KEY_ENDED_SEEN = booleanPreferencesKey("pro_trial_ended_seen")
        val KEY_REMOVED_SEEN = booleanPreferencesKey("pro_trial_removed_seen")
    }
}
