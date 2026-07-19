package com.dmytrosamoilov.offhand.feature.recording.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.domain.SessionPhase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.recording.service.RecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: RecordingSessionManager,
    observeDeveloperOptions: ObserveDeveloperOptionsUseCase,
) : BaseViewModel() {

    private val waveform = MutableStateFlow<List<Float>>(emptyList())

    val uiState: StateFlow<RecordingUiState> = combine(
        sessionManager.session,
        sessionManager.vad,
        waveform,
        observeDeveloperOptions(),
        sessionManager.externalMicName,
        ::toRecordingUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = RecordingUiState(),
    )

    init {
        viewModelScope.launch { collectWaveform() }
    }

    fun onStartRecording() {
        waveform.value = emptyList()
        RecordingService.start(context)
    }

    fun onPauseRecording() {
        sessionManager.pause()
    }

    fun onResumeRecording() {
        sessionManager.resume()
    }

    fun onStopRecording() {
        sessionManager.stop()
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
