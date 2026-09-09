package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.DeleteCustomNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveCustomNoteStylesUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveEntitlementsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoteStylesViewModel(
    observeCustomNoteStyles: ObserveCustomNoteStylesUseCase,
    observeEntitlements: ObserveEntitlementsUseCase,
    private val deleteCustomNoteStyle: DeleteCustomNoteStyleUseCase,
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
            observeEntitlements().collect { entitlements ->
                mutableUiState.update { it.copy(isUnlocked = entitlements.customStylesUnlocked) }
            }
        }
    }

    fun onDeleteRequested(id: Long) {
        mutableUiState.update { it.copy(pendingDeleteId = id) }
    }

    fun onDeleteDismissed() {
        mutableUiState.update { it.copy(pendingDeleteId = null) }
    }

    fun onDeleteConfirmed() {
        val id = mutableUiState.value.pendingDeleteId ?: return
        mutableUiState.update { it.copy(pendingDeleteId = null) }
        launchSafely(showLoading = false) {
            deleteCustomNoteStyle(id)
        }
    }
}
