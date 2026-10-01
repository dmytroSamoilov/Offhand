package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.StateFlow

interface AppForegroundState {
    val isInForeground: StateFlow<Boolean>
}
