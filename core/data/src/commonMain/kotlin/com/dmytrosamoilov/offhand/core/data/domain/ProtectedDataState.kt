package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

// Whether the app may open its protected files right now. On iOS the notes
// database and the recordings are protected "complete unless open": a file
// already open stays readable while the device is locked, but a new open
// fails until it is unlocked, so work that would open one waits for this.
interface ProtectedDataState {
    val isAvailable: StateFlow<Boolean>
}

suspend fun ProtectedDataState.awaitAvailable() {
    isAvailable.first { it }
}

// Android's app storage is readable whenever the app can run at all.
class AlwaysAvailableProtectedData : ProtectedDataState {
    override val isAvailable: StateFlow<Boolean> = MutableStateFlow(true)
}
