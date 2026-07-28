package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.ui.component.NotePresetOption
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveNotePresetUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetNotePresetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeDynamicColor: ObserveDynamicColorUseCase,
    private val setDynamicColor: SetDynamicColorUseCase,
    observeNotePreset: ObserveNotePresetUseCase,
    private val setNotePreset: SetNotePresetUseCase,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeDynamicColor().collect { enabled ->
                mutableUiState.update { it.copy(isDynamicColorEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            observeNotePreset().collect { preset ->
                mutableUiState.update { it.copy(notePreset = preset.toUi()) }
            }
        }
    }

    fun onNotePresetSelected(preset: NotePresetOption) {
        launchSafely(showLoading = false) {
            setNotePreset(preset.toDomain())
        }
    }

    fun onDynamicColorChanged(enabled: Boolean) {
        launchSafely(showLoading = false) {
            setDynamicColor(enabled)
        }
    }
}

private fun NotePreset.toUi(): NotePresetOption = when (this) {
    NotePreset.SUMMARY -> NotePresetOption.SUMMARY
    NotePreset.MEETING -> NotePresetOption.MEETING
    NotePreset.VISIT -> NotePresetOption.VISIT
    NotePreset.LEGAL -> NotePresetOption.LEGAL
}

private fun NotePresetOption.toDomain(): NotePreset = when (this) {
    NotePresetOption.SUMMARY -> NotePreset.SUMMARY
    NotePresetOption.MEETING -> NotePreset.MEETING
    NotePresetOption.VISIT -> NotePreset.VISIT
    NotePresetOption.LEGAL -> NotePreset.LEGAL
}
