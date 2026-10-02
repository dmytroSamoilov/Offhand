package com.dmytrosamoilov.offhand.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

// The on-device AI is a 2.4 GB download: it starts by itself on an unmetered
// network, and on mobile data only once the user has asked for it in this
// process.
class ModelDownloadPolicy(
    private val networkMonitor: NetworkMonitor,
) {

    private val mutableMobileDataAllowed = MutableStateFlow(false)
    val isMobileDataAllowed: StateFlow<Boolean> = mutableMobileDataAllowed.asStateFlow()

    val canStart: Flow<Boolean> = combine(networkMonitor.isUnmetered, isMobileDataAllowed) { unmetered, allowed ->
        unmetered || allowed
    }

    fun canStartNow(): Boolean = networkMonitor.isUnmetered.value || isMobileDataAllowed.value

    fun allowMobileData() {
        mutableMobileDataAllowed.value = true
    }
}
