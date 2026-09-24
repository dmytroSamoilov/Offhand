package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.feature.settings.R
import org.koin.androidx.compose.koinViewModel

// Mounted once at the root: audio shared from another app can arrive on any screen.
@Composable
fun SharedAudioImportHost(viewModel: SharedAudioImportViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isProDialogShown) {
        ProImportDialog(
            onUpgrade = viewModel::onUpgradeClicked,
            onDecline = viewModel::onImportDeclined,
        )
    }
    ImportNotice(notice = state.notice, onDismiss = viewModel::onNoticeDismissed)
}

@Composable
private fun ProImportDialog(onUpgrade: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text(text = stringResource(R.string.settings_import_pro_title)) },
        text = { Text(text = stringResource(R.string.settings_import_pro_body)) },
        confirmButton = {
            TextButton(onClick = onUpgrade) { Text(text = stringResource(R.string.settings_pro_upgrade)) }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text(text = stringResource(R.string.settings_import_pro_cancel)) }
        },
    )
}
