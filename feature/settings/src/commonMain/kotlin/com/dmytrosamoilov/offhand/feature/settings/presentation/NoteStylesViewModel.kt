package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.core.data.domain.requiredProFeature
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.IsCustomNoteStylesAvailableUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveCustomNoteStylesUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetNoteStyleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// One screen picks the default summary style and opens the editor for the custom ones.
class NoteStylesViewModel(
    observeCustomNoteStyles: ObserveCustomNoteStylesUseCase,
    isCustomNoteStylesAvailable: IsCustomNoteStylesAvailableUseCase,
    observeNoteStyle: ObserveNoteStyleUseCase,
    private val setNoteStyle: SetNoteStyleUseCase,
    private val proUpgradeGate: ProUpgradeGate,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(NoteStylesUiState())
    val uiState: StateFlow<NoteStylesUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeCustomNoteStyles().collect { styles ->
                mutableUiState.update { it.copy(customStyles = styles.map { style -> style.toOptionUi() }) }
            }
        }
        viewModelScope.launch {
            isCustomNoteStylesAvailable().collect { unlocked ->
                mutableUiState.update { it.copy(isUnlocked = unlocked) }
            }
        }
        viewModelScope.launch {
            observeNoteStyle().collect { style ->
                mutableUiState.update { it.copy(selected = style) }
            }
        }
    }

    // Summary is free; every other default goes through the paywall.
    fun onStyleSelected(style: NoteStyleRef) {
        launchSafely(showLoading = false) {
            val feature = style.requiredProFeature()
            if (feature != null && !proUpgradeGate.requirePro(feature)) return@launchSafely
            setNoteStyle(style)
            analyticsTracker.track(AnalyticsEvents.defaultStyleChanged(style))
        }
    }
}
