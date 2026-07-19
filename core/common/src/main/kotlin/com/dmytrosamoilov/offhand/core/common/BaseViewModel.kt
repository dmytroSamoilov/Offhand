package com.dmytrosamoilov.offhand.core.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    private val mutableIsLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = mutableIsLoading.asStateFlow()

    private val mutableErrorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = mutableErrorMessage.asStateFlow()

    protected fun launchSafely(
        showLoading: Boolean = true,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = viewModelScope.launch {
        if (showLoading) mutableIsLoading.value = true
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            mutableErrorMessage.value = throwable.message ?: throwable.javaClass.simpleName
        } finally {
            if (showLoading) mutableIsLoading.value = false
        }
    }

    fun dismissError() {
        mutableErrorMessage.value = null
    }
}
