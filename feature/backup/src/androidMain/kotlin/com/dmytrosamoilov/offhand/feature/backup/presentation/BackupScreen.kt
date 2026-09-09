package com.dmytrosamoilov.offhand.feature.backup.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.component.AppTopBar
import com.dmytrosamoilov.offhand.core.designsystem.theme.extendedColors
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.feature.backup.R
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupFileNames
import com.dmytrosamoilov.offhand.feature.backup.domain.UriBackupFile
import org.koin.androidx.compose.koinViewModel
import com.dmytrosamoilov.offhand.core.designsystem.haptics.haptics

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
        Scaffold(
            topBar = { BackupTopBar(onBack = onBack) },
            contentWindowInsets = WindowInsets(0.dp),
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionCard(title = stringResource(R.string.backup_create_title), body = stringResource(R.string.backup_create_body)) {
                    Button(onClick = viewModel::onBackupFlowStarted, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.backup_start_button))
                    }
                }
                SectionCard(title = stringResource(R.string.backup_restore_title), body = stringResource(R.string.backup_restore_body)) {
                    Button(onClick = { openDocument.launch(arrayOf(ANY_MIME_TYPE)) }, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.backup_restore_button))
                    }
                }
            }
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
private fun BackupTopBar(onBack: () -> Unit) {
    AppTopBar(
        title = stringResource(R.string.backup_title),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.backup_back_description),
                )
            }
        },
    )
}

@Composable
private fun SectionCard(title: String, body: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
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
    ModalBottomSheet(onDismissRequest = viewModel::onBackupFlowDismissed, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val operation = state.operation) {
                null -> BackupStepContent(state = state, viewModel = viewModel, onSave = onSave)
                is BackupOperationUi.Running -> BackupProgressContent(percent = operation.percent)
                else -> BackupResultContent(operation = operation, onDone = viewModel::onBackupFlowDismissed)
            }
        }
    }
}

@Composable
private fun BackupStepContent(state: BackupUiState, viewModel: BackupViewModel, onSave: () -> Unit) {
    when (state.backupStep) {
        BackupStepUi.PASSPHRASE -> PassphraseStep(state = state, viewModel = viewModel)
        BackupStepUi.OPTIONS -> OptionsStep(state = state, viewModel = viewModel, onSave = onSave)
        null -> Unit
    }
}

@Composable
private fun PassphraseStep(state: BackupUiState, viewModel: BackupViewModel) {
    Text(text = stringResource(R.string.backup_step_passphrase_title), style = MaterialTheme.typography.titleLarge)
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
    )
    WarningBox(text = stringResource(R.string.backup_warning))
    Button(onClick = viewModel::onBackupPassphraseContinued, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.backup_continue))
    }
}

@Composable
private fun OptionsStep(state: BackupUiState, viewModel: BackupViewModel, onSave: () -> Unit) {
    Text(text = stringResource(R.string.backup_step_options_title), style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = state.backupFileName,
        onValueChange = viewModel::onBackupFileNameChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(R.string.backup_file_name_label)) },
        suffix = { Text(text = BackupFileNames.EXTENSION) },
        singleLine = true,
    )
    IncludeAudioRow(checked = state.includeAudio, onCheckedChange = viewModel::onIncludeAudioChanged)
    Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.backup_save_button))
    }
}

@Composable
private fun BackupProgressContent(percent: Int) {
    Text(text = stringResource(R.string.backup_progress_backup), style = MaterialTheme.typography.titleLarge)
    LinearProgressIndicator(progress = { percent / PERCENT_SCALE }, modifier = Modifier.fillMaxWidth())
    Text(text = stringResource(R.string.backup_progress_percent, percent))
}

@Composable
private fun BackupResultContent(operation: BackupOperationUi, onDone: () -> Unit) {
    Text(text = operation.title(), style = MaterialTheme.typography.titleLarge)
    OperationBody(operation = operation)
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.backup_done))
    }
}

@Composable
private fun WarningBox(text: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.extendedColors.warningContainer,
        contentColor = MaterialTheme.extendedColors.onWarningContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Filled.WarningAmber, contentDescription = null)
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PassphraseField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: PassphraseErrorUi?,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(text = it.message()) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}

@Composable
private fun PassphraseErrorUi.message(): String = when (this) {
    PassphraseErrorUi.TOO_SHORT -> stringResource(R.string.backup_passphrase_too_short, BackupUiState.MIN_PASSPHRASE_LENGTH)
    PassphraseErrorUi.MISMATCH -> stringResource(R.string.backup_passphrase_mismatch)
}

@Composable
private fun IncludeAudioRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.backup_include_audio_label), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.backup_include_audio_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        val haptics = haptics()
        Switch(
            checked = checked,
            onCheckedChange = { isOn ->
                haptics.toggle(isOn)
                onCheckedChange(isOn)
            },
        )
    }
}

@Composable
private fun RestorePassphraseDialog(state: BackupUiState, viewModel: BackupViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::onRestorePassphraseDismissed,
        title = { Text(text = stringResource(R.string.backup_restore_dialog_title)) },
        text = {
            PassphraseField(
                value = state.passphrase,
                onValueChange = viewModel::onPassphraseChanged,
                label = stringResource(R.string.backup_passphrase_label),
                error = state.passphraseError,
            )
        },
        confirmButton = {
            TextButton(onClick = viewModel::onRestoreConfirmed) {
                Text(text = stringResource(R.string.backup_restore_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::onRestorePassphraseDismissed) {
                Text(text = stringResource(R.string.backup_cancel))
            }
        },
    )
}

@Composable
private fun RestoreOperationDialog(operation: BackupOperationUi, onDismiss: () -> Unit) {
    val isRunning = operation is BackupOperationUi.Running
    AlertDialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !isRunning, dismissOnClickOutside = !isRunning),
        title = { Text(text = operation.title()) },
        text = { OperationBody(operation = operation) },
        confirmButton = {
            if (!isRunning) {
                TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.backup_ok)) }
            }
        },
    )
}

@Composable
private fun OperationBody(operation: BackupOperationUi) {
    when (operation) {
        is BackupOperationUi.Running -> Column {
            LinearProgressIndicator(progress = { operation.percent / PERCENT_SCALE }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = stringResource(R.string.backup_progress_percent, operation.percent))
        }
        is BackupOperationUi.BackupCompleted -> Text(
            text = stringResource(R.string.backup_done_backup_body, operation.notes, operation.folders),
        )
        is BackupOperationUi.RestoreCompleted -> Text(
            text = stringResource(
                R.string.backup_done_restore_body,
                operation.notesRestored,
                operation.notesSkipped,
                operation.foldersCreated,
                operation.foldersReused,
            ),
        )
        is BackupOperationUi.Failed -> Text(text = operation.error.message())
    }
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
