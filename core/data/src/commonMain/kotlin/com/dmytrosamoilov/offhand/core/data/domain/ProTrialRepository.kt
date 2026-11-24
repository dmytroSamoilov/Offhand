package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.Flow

interface ProTrialRepository {

    // Turns to Ended by itself when the trial runs out while the app is open.
    fun observeState(): Flow<ProTrialState>

    suspend fun currentState(): ProTrialState

    suspend fun readRecord(): ProTrialRecord?

    suspend fun writeRecord(record: ProTrialRecord)

    // Called on a cold start, so a clock set back later cannot rewind the trial.
    suspend fun rememberCurrentTime()

    suspend fun isNoticeSeen(notice: ProTrialNotice): Boolean

    suspend fun markNoticeSeen(notice: ProTrialNotice)

    // Suspends for as long as the app runs, folding in records that arrive
    // from outside.
    suspend fun keepInSync()
}
