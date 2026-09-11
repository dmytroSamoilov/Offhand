package com.dmytrosamoilov.offhand.feature.backup.presentation

import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupException
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupFile
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupFileNames
import com.dmytrosamoilov.offhand.feature.backup.domain.usecase.CreateBackupUseCase
import com.dmytrosamoilov.offhand.feature.backup.domain.usecase.RestoreBackupUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BackupViewModel(
    private val createBackup: CreateBackupUseCase,
    private val restoreBackup: RestoreBackupUseCase,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = mutableUiState.asStateFlow()

    private var pendingRestore: BackupFile? = null

    fun onIncludeAudioChanged(enabled: Boolean) {
        mutableUiState.update { it.copy(includeAudio = enabled) }
    }

    fun onIncludeStylesChanged(enabled: Boolean) {
        mutableUiState.update { it.copy(includeStyles = enabled) }
    }

    fun onPassphraseChanged(value: String) {
        mutableUiState.update { it.copy(passphrase = value, passphraseError = null) }
    }

    fun onPassphraseConfirmationChanged(value: String) {
        mutableUiState.update { it.copy(passphraseConfirmation = value, passphraseError = null) }
    }

    fun validateBackupPassphrase(): Boolean {
        val state = mutableUiState.value
        val error = when {
            state.passphrase.length < BackupUiState.MIN_PASSPHRASE_LENGTH -> PassphraseErrorUi.TOO_SHORT
            state.passphrase != state.passphraseConfirmation -> PassphraseErrorUi.MISMATCH
            else -> null
        }
        mutableUiState.update { it.copy(passphraseError = error) }
        return error == null
    }

    fun onBackupFlowStarted() {
        mutableUiState.update {
            it.copy(backupStep = BackupStepUi.PASSPHRASE, passphrase = "", passphraseConfirmation = "", passphraseError = null)
        }
    }

    fun onBackupPassphraseContinued() {
        if (!validateBackupPassphrase()) return
        mutableUiState.update {
            it.copy(backupStep = BackupStepUi.OPTIONS, backupFileName = BackupFileNames.suggested())
        }
    }

    fun onBackupFileNameChanged(name: String) {
        mutableUiState.update { it.copy(backupFileName = name) }
    }

    fun onBackupFlowDismissed() {
        if (mutableUiState.value.operation is BackupOperationUi.Running) return
        mutableUiState.update {
            it.copy(backupStep = null, passphrase = "", passphraseConfirmation = "", passphraseError = null, operation = null)
        }
    }

    fun onBackupTargetChosen(file: BackupFile) {
        if (!validateBackupPassphrase()) return
        val includeAudio = mutableUiState.value.includeAudio
        val includeStyles = mutableUiState.value.includeStyles
        runOperation(BackupModeUi.BACKUP) { passphrase, report ->
            val summary = createBackup(file, passphrase, includeAudio, includeStyles, report)
            analyticsTracker.track(AnalyticsEvents.backupCreated(summary.notes, includeAudio))
            BackupOperationUi.BackupCompleted(summary.notes, summary.folders, summary.audioBytes)
        }
    }

    fun onRestoreSourceChosen(file: BackupFile) {
        pendingRestore = file
        mutableUiState.update { it.copy(isRestorePassphraseRequested = true, passphraseError = null) }
    }

    fun onRestorePassphraseDismissed() {
        pendingRestore = null
        mutableUiState.update { it.copy(isRestorePassphraseRequested = false) }
    }

    fun onRestoreConfirmed() {
        val file = pendingRestore ?: return
        if (mutableUiState.value.passphrase.isEmpty()) {
            mutableUiState.update { it.copy(passphraseError = PassphraseErrorUi.TOO_SHORT) }
            return
        }
        pendingRestore = null
        mutableUiState.update { it.copy(isRestorePassphraseRequested = false) }
        runOperation(BackupModeUi.RESTORE) { passphrase, report ->
            val summary = restoreBackup(file, passphrase, report)
            analyticsTracker.track(AnalyticsEvents.backupRestored(summary.notesRestored))
            BackupOperationUi.RestoreCompleted(
                notesRestored = summary.notesRestored,
                notesSkipped = summary.notesSkipped,
                foldersCreated = summary.foldersCreated,
                foldersReused = summary.foldersReused,
            )
        }
    }

    fun onOperationDismissed() {
        mutableUiState.update { it.copy(operation = null) }
    }

    private fun runOperation(
        mode: BackupModeUi,
        block: suspend (passphrase: ByteArray, report: (Int) -> Unit) -> BackupOperationUi,
    ) {
        val passphrase = mutableUiState.value.passphrase.encodeToByteArray()
        mutableUiState.update { it.copy(operation = BackupOperationUi.Running(mode, percent = 0)) }
        viewModelScope.launch {
            val result = try {
                block(passphrase) { percent ->
                    mutableUiState.update { it.copy(operation = BackupOperationUi.Running(mode, percent)) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                BackupOperationUi.Failed(mode, failure.toErrorUi())
            } finally {
                passphrase.fill(0)
            }
            mutableUiState.update { it.copy(operation = result, passphrase = "", passphraseConfirmation = "") }
        }
    }

    private fun Exception.toErrorUi(): BackupErrorUi = when (this) {
        is BackupException.WrongPassphrase -> BackupErrorUi.WRONG_PASSPHRASE
        is BackupException.UnsupportedVersion -> BackupErrorUi.UNSUPPORTED_VERSION
        is BackupException.Corrupt -> BackupErrorUi.CORRUPT_FILE
        else -> BackupErrorUi.IO
    }
}
