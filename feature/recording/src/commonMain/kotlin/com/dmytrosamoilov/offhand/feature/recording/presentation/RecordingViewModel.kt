package com.dmytrosamoilov.offhand.feature.recording.presentation

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.domain.SessionPhase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.MarkNotificationsPromptedUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ObserveNotificationsPromptedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecordingViewModel(
    private val recordingProcessController: RecordingProcessController,
    private val sessionManager: RecordingSessionManager,
    observeDeveloperOptions: ObserveDeveloperOptionsUseCase,
    observeNotificationsPrompted: ObserveNotificationsPromptedUseCase,
    private val markNotificationsPrompted: MarkNotificationsPromptedUseCase,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel() {

    private val waveform = MutableStateFlow<List<Float>>(emptyList())
    private val isNotificationPromptRequested = MutableStateFlow(false)
    private val wasNotificationPrompted = observeNotificationsPrompted()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val uiState: StateFlow<RecordingUiState> = combine(
        sessionManager.session,
        sessionManager.vad,
        waveform,
        observeDeveloperOptions(),
        sessionManager.externalMicName,
        ::toRecordingUiState,
    ).combine(isNotificationPromptRequested) { state, isPromptRequested ->
        state.copy(isNotificationPromptRequested = isPromptRequested)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = RecordingUiState(),
    )

    init {
        viewModelScope.launch { collectWaveform() }
    }

    fun onStartRecording(folderId: Long?) {
        waveform.value = emptyList()
        recordingProcessController.startRecording(folderId)
    }

    fun onPauseRecording() {
        sessionManager.pause()
    }

    fun onResumeRecording() {
        sessionManager.resume()
    }

    // The first finished recording is the moment to ask for notifications: the
    // user has just made something worth being told about.
    fun onStopRecording() {
        sessionManager.stop()
        if (!wasNotificationPrompted.value) isNotificationPromptRequested.value = true
    }

    // Read right after onStopRecording by the iOS sheet, which closes before the
    // combined state would have caught up.
    val isNotificationPromptPending: Boolean
        get() = isNotificationPromptRequested.value

    fun onNotificationPromptAnswered(granted: Boolean) {
        analyticsTracker.track(AnalyticsEvents.notificationPermission(granted))
        onNotificationPromptSkipped()
    }

    // The platform had nothing to ask (already decided, or no permission on
    // this OS version), so only remember that the moment has passed.
    fun onNotificationPromptSkipped() {
        isNotificationPromptRequested.value = false
        launchSafely(showLoading = false) { markNotificationsPrompted() }
    }

    fun onDiscardRecording() {
        sessionManager.discard()
    }

    fun onSheetOpened() {
        sessionManager.resetToIdle()
    }

    fun onSheetClosed() {
        sessionManager.resetToIdle()
    }

    private suspend fun collectWaveform() {
        sessionManager.vad.collect { vad ->
            val session = sessionManager.session.value
            if (session.phase == SessionPhase.RECORDING && !session.isPaused && !vad.isPaused) {
                waveform.update { history ->
                    (history + normalizedAudioLevel(vad.rmsDb)).takeLast(WAVEFORM_BAR_CAPACITY)
                }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val WAVEFORM_BAR_CAPACITY = 150
    }
}
