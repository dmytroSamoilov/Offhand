package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandDialog
import com.dmytrosamoilov.offhand.feature.settings.R
import org.koin.androidx.compose.koinViewModel

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
    OffhandDialog(
        title = stringResource(R.string.settings_import_pro_title),
        body = stringResource(R.string.settings_import_pro_body),
        onDismissRequest = onDecline,
        confirmLabel = stringResource(R.string.settings_pro_upgrade),
        onConfirm = onUpgrade,
        dismissLabel = stringResource(R.string.settings_import_pro_cancel),
    )
}
