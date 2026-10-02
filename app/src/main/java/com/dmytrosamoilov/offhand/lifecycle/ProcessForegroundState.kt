package com.dmytrosamoilov.offhand.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.dmytrosamoilov.offhand.core.data.domain.AppForegroundState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Created on the main thread at application start, because the process
// lifecycle only accepts observers there.
class ProcessForegroundState : AppForegroundState, DefaultLifecycleObserver {

    private val mutableIsInForeground = MutableStateFlow(false)
    override val isInForeground: StateFlow<Boolean> = mutableIsInForeground.asStateFlow()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        mutableIsInForeground.value = true
    }

    override fun onStop(owner: LifecycleOwner) {
        mutableIsInForeground.value = false
    }
}
