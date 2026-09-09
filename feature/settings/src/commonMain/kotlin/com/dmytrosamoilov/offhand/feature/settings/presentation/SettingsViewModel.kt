package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.IsCustomNoteStylesAvailableUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveCustomNoteStylesUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetNoteStyleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeDynamicColor: ObserveDynamicColorUseCase,
    private val setDynamicColor: SetDynamicColorUseCase,
    observeNoteStyle: ObserveNoteStyleUseCase,
    private val setNoteStyle: SetNoteStyleUseCase,
    observeCustomNoteStyles: ObserveCustomNoteStylesUseCase,
    isCustomNoteStylesAvailable: IsCustomNoteStylesAvailableUseCase,
    observeAppLockEnabled: ObserveAppLockEnabledUseCase,
    private val setAppLockEnabled: SetAppLockEnabledUseCase,
    private val appLockManager: AppLockManager,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(
        SettingsUiState(isDeviceSecure = appLockManager.isDeviceSecure),
    )
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeDynamicColor().collect { enabled ->
                mutableUiState.update { it.copy(isDynamicColorEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            observeNoteStyle().collect { style ->
                mutableUiState.update { it.copy(noteStyle = style) }
            }
        }
        viewModelScope.launch {
            combine(observeCustomNoteStyles(), isCustomNoteStylesAvailable()) { styles, unlocked ->
                styles.takeIf { unlocked }.orEmpty().map { style -> style.toOptionUi() } to unlocked
            }.collect { (styles, unlocked) ->
                mutableUiState.update { it.copy(customStyles = styles, isCustomStylesUnlocked = unlocked) }
            }
        }
        viewModelScope.launch {
            observeAppLockEnabled().collect { enabled ->
                mutableUiState.update { it.copy(isAppLockEnabled = enabled) }
            }
        }
    }

    // A passcode can be added or removed in system settings while this screen is
    // backgrounded, and neither change notifies the app.
    fun onScreenShown() {
        mutableUiState.update { it.copy(isDeviceSecure = appLockManager.isDeviceSecure) }
    }

    fun onNoteStyleSelected(style: NoteStyleRef) {
        launchSafely(showLoading = false) {
            setNoteStyle(style)
        }
    }

    fun onDynamicColorChanged(enabled: Boolean) {
        launchSafely(showLoading = false) {
            setDynamicColor(enabled)
        }
    }

    fun onAppLockChanged(enabled: Boolean) {
        launchSafely(showLoading = false) {
            setAppLockEnabled(enabled && appLockManager.isDeviceSecure)
        }
    }
}
