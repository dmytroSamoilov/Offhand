package com.dmytrosamoilov.offhand.feature.backup.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.component.BarIconButton
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandDialog
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandSheet
import com.dmytrosamoilov.offhand.core.designsystem.component.PrimaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.ProgressLine
import com.dmytrosamoilov.offhand.core.designsystem.component.ScreenTitle
import com.dmytrosamoilov.offhand.core.designsystem.component.SecondaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.SurfaceCard
import com.dmytrosamoilov.offhand.core.designsystem.component.SwitchRow
import com.dmytrosamoilov.offhand.core.designsystem.component.TopBar
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.core.ui.component.LabeledTextField
import com.dmytrosamoilov.offhand.core.designsystem.component.SectionLabel
import com.dmytrosamoilov.offhand.feature.backup.R
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupFileNames
import com.dmytrosamoilov.offhand.feature.backup.domain.UriBackupFile
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val contentResolver = LocalContext.current.applicationContext.contentResolver
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)) { uri ->
        uri?.let { viewModel.onBackupTargetChosen(UriBackupFile(contentResolver, it)) }
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onRestoreSourceChosen(UriBackupFile(contentResolver, it)) }
    }

    BaseComposeScreen(viewModel = viewModel, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                leading = {
                    BarIconButton(
                        icon = OffhandIcons.Back,
                        contentDescription = stringResource(R.string.backup_back_description),
                        onClick = onBack,
                    )
                },
            )
            BackupContent(onBackup = viewModel::onBackupFlowStarted, onRestore = { openDocument.launch(arrayOf(ANY_MIME_TYPE)) })
        }
    }

    if (state.backupStep != null) {
        BackupSheet(
            state = state,
            viewModel = viewModel,
            onSave = { createDocument.launch(BackupFileNames.withExtension(state.backupFileName)) },
        )
    }
    if (state.isRestorePassphraseRequested) {
        RestorePassphraseDialog(state = state, viewModel = viewModel)
    }
    state.operation?.takeIf { it.mode == BackupModeUi.RESTORE }?.let { operation ->
        RestoreOperationDialog(operation = operation, onDismiss = viewModel::onOperationDismissed)
    }
}

@Composable
private fun BackupContent(onBackup: () -> Unit, onRestore: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = OffhandSpacing.screen, end = OffhandSpacing.screen, top = 8.dp, bottom = 32.dp),
    ) {
        ScreenTitle(title = stringResource(R.string.backup_title), subtitle = stringResource(R.string.backup_subtitle))
        GroupLabel(text = stringResource(R.string.backup_create_title))
        PrimaryButton(text = stringResource(R.string.backup_start_button), onClick = onBackup, modifier = Modifier.fillMaxWidth())
        Hint(text = stringResource(R.string.backup_create_body))
        GroupLabel(text = stringResource(R.string.backup_restore_title))
        SecondaryButton(text = stringResource(R.string.backup_restore_button), onClick = onRestore, modifier = Modifier.fillMaxWidth())
        Hint(text = stringResource(R.string.backup_restore_body))
    }
}

@Composable
private fun GroupLabel(text: String) {
    SectionLabel(text = text, modifier = Modifier.padding(top = 26.dp, bottom = 10.dp))
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = OffhandTheme.type.caption,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupSheet(
    state: BackupUiState,
    viewModel: BackupViewModel,
    onSave: () -> Unit,
) {
    val isRunning = state.operation is BackupOperationUi.Running
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !isRunning },
    )
    val operation = state.operation
    OffhandSheet(
        onDismissRequest = viewModel::onBackupFlowDismissed,
        sheetState = sheetState,
        title = operation?.title() ?: state.backupStep?.title(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (operation) {
                null -> BackupStepContent(state = state, viewModel = viewModel, onSave = onSave)
                is BackupOperationUi.Running -> RunningContent(percent = operation.percent)
                else -> BackupResultContent(operation = operation, onDone = viewModel::onBackupFlowDismissed)
            }
        }
    }
}

@Composable
private fun BackupStepUi.title(): String = stringResource(
    when (this) {
        BackupStepUi.PASSPHRASE -> R.string.backup_step_passphrase_title
        BackupStepUi.OPTIONS -> R.string.backup_step_options_title
    },
)

@Composable
private fun ColumnScope.BackupStepContent(state: BackupUiState, viewModel: BackupViewModel, onSave: () -> Unit) {
    when (state.backupStep) {
        BackupStepUi.PASSPHRASE -> PassphraseStep(state = state, viewModel = viewModel)
        BackupStepUi.OPTIONS -> OptionsStep(state = state, viewModel = viewModel, onSave = onSave)
        null -> Unit
    }
}

@Composable
private fun PassphraseStep(state: BackupUiState, viewModel: BackupViewModel) {
    PassphraseField(
        value = state.passphrase,
        onValueChange = viewModel::onPassphraseChanged,
        label = stringResource(R.string.backup_passphrase_label),
        error = state.passphraseError?.takeIf { it == PassphraseErrorUi.TOO_SHORT },
    )
    PassphraseField(
        value = state.passphraseConfirmation,
        onValueChange = viewModel::onPassphraseConfirmationChanged,
        label = stringResource(R.string.backup_passphrase_confirm_label),
        error = state.passphraseError?.takeIf { it == PassphraseErrorUi.MISMATCH },
        help = stringResource(R.string.backup_warning),
    )
    PrimaryButton(
        text = stringResource(R.string.backup_continue),
        onClick = viewModel::onBackupPassphraseContinued,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OptionsStep(state: BackupUiState, viewModel: BackupViewModel, onSave: () -> Unit) {
    LabeledTextField(
        label = stringResource(R.string.backup_file_name_label),
        value = state.backupFileName,
        onValueChange = viewModel::onBackupFileNameChanged,
        trailing = {
            Text(
                text = BackupFileNames.EXTENSION,
                style = OffhandTheme.type.body,
                color = OffhandTheme.colors.ink3,
                modifier = Modifier.padding(end = 12.dp),
            )
        },
    )
    SurfaceCard {
        SwitchRow(
            title = stringResource(R.string.backup_include_audio_label),
            subtitle = stringResource(R.string.backup_include_audio_description),
            checked = state.includeAudio,
            onCheckedChange = viewModel::onIncludeAudioChanged,
            showProBadge = !state.isAudioUnlocked,
        )
        SwitchRow(
            title = stringResource(R.string.backup_include_styles_label),
            subtitle = stringResource(R.string.backup_include_styles_description),
            checked = state.includeStyles,
            onCheckedChange = viewModel::onIncludeStylesChanged,
            showDivider = false,
        )
    }
    PrimaryButton(text = stringResource(R.string.backup_save_button), onClick = onSave, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun RunningContent(percent: Int) {
    ProgressLine(progress = percent / PERCENT_SCALE, thickness = 3.dp)
    Text(
        text = stringResource(R.string.backup_progress_percent, percent),
        style = OffhandTheme.type.number,
        color = OffhandTheme.colors.ink2,
    )
}

@Composable
private fun BackupResultContent(operation: BackupOperationUi, onDone: () -> Unit) {
    OperationBody(operation = operation)
    PrimaryButton(text = stringResource(R.string.backup_done), onClick = onDone, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PassphraseField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: PassphraseErrorUi?,
    help: String? = null,
    focusesOnAppear: Boolean = false,
) {
    LabeledTextField(
        focusesOnAppear = focusesOnAppear,
        label = label,
        value = value,
        onValueChange = onValueChange,
        error = error?.message(),
        help = help,
        keyboardType = KeyboardType.Password,
        visualTransformation = PasswordVisualTransformation(),
    )
}

@Composable
private fun PassphraseErrorUi.message(): String = when (this) {
    PassphraseErrorUi.TOO_SHORT -> stringResource(R.string.backup_passphrase_too_short, BackupUiState.MIN_PASSPHRASE_LENGTH)
    PassphraseErrorUi.MISMATCH -> stringResource(R.string.backup_passphrase_mismatch)
}

@Composable
private fun RestorePassphraseDialog(state: BackupUiState, viewModel: BackupViewModel) {
    OffhandDialog(
        title = stringResource(R.string.backup_restore_dialog_title),
        onDismissRequest = viewModel::onRestorePassphraseDismissed,
        confirmLabel = stringResource(R.string.backup_restore_dialog_confirm),
        onConfirm = viewModel::onRestoreConfirmed,
        dismissLabel = stringResource(R.string.backup_cancel),
    ) {
        Column(modifier = Modifier.padding(top = 16.dp)) {
            PassphraseField(
                value = state.passphrase,
                onValueChange = viewModel::onPassphraseChanged,
                label = stringResource(R.string.backup_passphrase_label),
                error = state.passphraseError,
                focusesOnAppear = true,
            )
        }
    }
}

@Composable
private fun RestoreOperationDialog(operation: BackupOperationUi, onDismiss: () -> Unit) {
    val isRunning = operation is BackupOperationUi.Running
    OffhandDialog(
        title = operation.title(),
        onDismissRequest = { if (!isRunning) onDismiss() },
        confirmLabel = if (isRunning) null else stringResource(R.string.backup_ok),
        onConfirm = onDismiss,
    ) {
        Column(modifier = Modifier.padding(top = 10.dp)) { OperationBody(operation = operation) }
    }
}

@Composable
private fun OperationBody(operation: BackupOperationUi) {
    when (operation) {
        is BackupOperationUi.Running -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RunningContent(percent = operation.percent)
        }
        is BackupOperationUi.BackupCompleted -> BodyText(
            text = stringResource(R.string.backup_done_backup_body, operation.notes, operation.folders),
        )
        is BackupOperationUi.RestoreCompleted -> BodyText(
            text = stringResource(
                R.string.backup_done_restore_body,
                operation.notesRestored,
                operation.notesSkipped,
                operation.foldersCreated,
                operation.foldersReused,
            ),
        )
        is BackupOperationUi.Failed -> BodyText(text = operation.error.message())
    }
}

@Composable
private fun BodyText(text: String) {
    Text(text = text, style = OffhandTheme.type.dialogBody, color = OffhandTheme.colors.ink2)
}

@Composable
private fun BackupOperationUi.title(): String = when (this) {
    is BackupOperationUi.Running -> stringResource(
        if (mode == BackupModeUi.BACKUP) R.string.backup_progress_backup else R.string.backup_progress_restore,
    )
    is BackupOperationUi.BackupCompleted -> stringResource(R.string.backup_done_backup_title)
    is BackupOperationUi.RestoreCompleted -> stringResource(R.string.backup_done_restore_title)
    is BackupOperationUi.Failed -> stringResource(
        if (mode == BackupModeUi.BACKUP) R.string.backup_failed_backup_title else R.string.backup_failed_restore_title,
    )
}

@Composable
private fun BackupErrorUi.message(): String = when (this) {
    BackupErrorUi.WRONG_PASSPHRASE -> stringResource(R.string.backup_error_wrong_passphrase)
    BackupErrorUi.CORRUPT_FILE -> stringResource(R.string.backup_error_corrupt)
    BackupErrorUi.UNSUPPORTED_VERSION -> stringResource(R.string.backup_error_unsupported)
    BackupErrorUi.IO -> stringResource(R.string.backup_error_io)
}

private const val BACKUP_MIME_TYPE = "application/octet-stream"
private const val ANY_MIME_TYPE = "*/*"
private const val PERCENT_SCALE = 100f
