package com.dmytrosamoilov.offhand.feature.backup.presentation

data class BackupUiState(
    val includeAudio: Boolean = true,
    val includeStyles: Boolean = true,
    val passphrase: String = "",
    val passphraseConfirmation: String = "",
    val passphraseError: PassphraseErrorUi? = null,
    val isRestorePassphraseRequested: Boolean = false,
    val backupStep: BackupStepUi? = null,
    val backupFileName: String = "",
    val operation: BackupOperationUi? = null,
) {
    val canStartBackup: Boolean
        get() = passphrase.length >= MIN_PASSPHRASE_LENGTH && passphrase == passphraseConfirmation

    companion object {
        const val MIN_PASSPHRASE_LENGTH = 8
    }
}

enum class BackupStepUi {
    PASSPHRASE,
    OPTIONS,
}

enum class PassphraseErrorUi {
    TOO_SHORT,
    MISMATCH,
}

enum class BackupModeUi {
    BACKUP,
    RESTORE,
}

sealed interface BackupOperationUi {
    val mode: BackupModeUi

    data class Running(override val mode: BackupModeUi, val percent: Int) : BackupOperationUi

    data class BackupCompleted(val notes: Int, val folders: Int, val audioBytes: Long) : BackupOperationUi {
        override val mode: BackupModeUi = BackupModeUi.BACKUP
    }

    data class RestoreCompleted(
        val notesRestored: Int,
        val notesSkipped: Int,
        val foldersCreated: Int,
        val foldersReused: Int,
    ) : BackupOperationUi {
        override val mode: BackupModeUi = BackupModeUi.RESTORE
    }

    data class Failed(override val mode: BackupModeUi, val error: BackupErrorUi) : BackupOperationUi
}

enum class BackupErrorUi {
    WRONG_PASSPHRASE,
    CORRUPT_FILE,
    UNSUPPORTED_VERSION,
    IO,
}
