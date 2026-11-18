package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.ProtectedDataState
import com.dmytrosamoilov.offhand.core.data.domain.awaitAvailable
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen

// A query that fails (on iOS, typically a new database connection refused
// while the device is locked) waits until the protected files can be opened
// again and runs once more, instead of failing every screen and background
// job that collects it. Nothing is lost: the next emission is a fresh read.
internal fun <T> Flow<T>.retryWhileUnreadable(protectedData: ProtectedDataState): Flow<T> =
    retryWhen { cause, attempt ->
        if (cause is CancellationException) return@retryWhen false
        protectedData.awaitAvailable()
        delay(retryDelayMs(attempt))
        true
    }

private fun retryDelayMs(attempt: Long): Long =
    (FIRST_RETRY_DELAY_MS shl attempt.coerceAtMost(MAX_DOUBLINGS).toInt()).coerceAtMost(MAX_RETRY_DELAY_MS)

private const val FIRST_RETRY_DELAY_MS = 500L
private const val MAX_DOUBLINGS = 6L
private const val MAX_RETRY_DELAY_MS = 30_000L
