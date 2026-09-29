package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.DiscardStagedAudioUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ImportAudioResult
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ImportAudioUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAudioImportAvailableUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

data class SharedAudioImportUiState(
    val pendingSources: List<AudioImportSource> = emptyList(),
    val pendingUnreadableCount: Int = 0,
    val notice: ImportNoticeUi? = null,
) {
    val isProDialogShown: Boolean
        get() = pendingSources.isNotEmpty()
}

// Audio handed over by another app: a Pro user's files go straight into the
// pipeline, a free user is asked first, because the share sheet gave no hint
// that this is a Pro feature, and only "Upgrade" leads to the paywall.
class SharedAudioImportViewModel(
    private val importAudio: ImportAudioUseCase,
    private val discardStagedAudio: DiscardStagedAudioUseCase,
    private val isAudioImportAvailable: IsAudioImportAvailableUseCase,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(SharedAudioImportUiState())
    val uiState: StateFlow<SharedAudioImportUiState> = mutableUiState.asStateFlow()

    fun onSharedAudioReceived(sources: List<AudioImportSource>, unreadableCount: Int) {
        if (sources.isEmpty() && unreadableCount == 0) return
        launchSafely(showLoading = false) {
            if (sources.isEmpty() || isAudioImportAvailable().first()) {
                start(sources, unreadableCount)
            } else {
                mutableUiState.update {
                    it.copy(
                        pendingSources = it.pendingSources + sources,
                        pendingUnreadableCount = it.pendingUnreadableCount + unreadableCount,
                    )
                }
            }
        }
    }

    fun onUpgradeClicked() {
        val (sources, unreadableCount) = takePending()
        launchSafely(showLoading = false) { start(sources, unreadableCount) }
    }

    fun onImportDeclined() {
        val (sources, _) = takePending()
        launchSafely(showLoading = false) { discardStagedAudio(sources) }
    }

    fun onNoticeDismissed() {
        mutableUiState.update { it.copy(notice = null) }
    }

    private suspend fun start(sources: List<AudioImportSource>, unreadableCount: Int) {
        val result = importAudio(sources)
        if (result == ImportAudioResult.LOCKED) discardStagedAudio(sources)
        val startedCount = if (result == ImportAudioResult.STARTED) sources.size else 0
        mutableUiState.update {
            it.copy(notice = importNoticeOf(hasUnreadable = unreadableCount > 0, startedCount = startedCount))
        }
    }

    private fun takePending(): Pair<List<AudioImportSource>, Int> {
        val state = mutableUiState.value
        mutableUiState.update { it.copy(pendingSources = emptyList(), pendingUnreadableCount = 0) }
        return state.pendingSources to state.pendingUnreadableCount
    }
}
