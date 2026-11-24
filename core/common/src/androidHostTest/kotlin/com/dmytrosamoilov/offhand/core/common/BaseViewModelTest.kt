package com.dmytrosamoilov.offhand.core.common

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class TestViewModel : BaseViewModel() {
        fun succeed() = launchSafely { }
        fun fail(message: String) = launchSafely { throw IllegalStateException(message) }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `launchSafely toggles loading around successful block`() = runTest(dispatcher) {
        val viewModel = TestViewModel()

        viewModel.isLoading.test {
            assertFalse(awaitItem())
            viewModel.succeed()
            assertTrue(awaitItem())
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `launchSafely surfaces failure as error message and stops loading`() = runTest(dispatcher) {
        val viewModel = TestViewModel()

        viewModel.fail("model not downloaded")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("model not downloaded", viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `dismissError clears error message`() = runTest(dispatcher) {
        val viewModel = TestViewModel()

        viewModel.fail("boom")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.dismissError()

        assertNull(viewModel.errorMessage.value)
    }
}
