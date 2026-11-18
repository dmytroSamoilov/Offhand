package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.ProtectedDataState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class UnreadableDatabaseRetryTest {

    private val isAvailable = MutableStateFlow(true)
    private val protectedData = object : ProtectedDataState {
        override val isAvailable: StateFlow<Boolean> = this@UnreadableDatabaseRetryTest.isAvailable
    }

    @Test
    fun `a failed read runs again and its next value arrives`() = runTest {
        var reads = 0
        val notes = flow {
            reads++
            if (reads == 1) error("unable to open database file")
            emit(listOf("note"))
        }

        assertEquals(listOf("note"), notes.retryWhileUnreadable(protectedData).first())
        assertEquals(2, reads)
    }

    @Test
    fun `a read that fails while the device is locked waits for the unlock`() = runTest {
        isAvailable.value = false
        var reads = 0
        val notes = flow {
            reads++
            if (reads == 1) error("unable to open database file")
            emit(listOf("note"))
        }

        val result = async { notes.retryWhileUnreadable(protectedData).first() }
        advanceUntilIdle()
        assertEquals(1, reads)

        isAvailable.value = true
        assertEquals(listOf("note"), result.await())
    }
}
