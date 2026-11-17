package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandDialog
import com.dmytrosamoilov.offhand.feature.notes.R

@Composable
internal fun DeleteNoteDialog(
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    @StringRes titleRes: Int = R.string.notes_delete_dialog_title,
    @StringRes bodyRes: Int = R.string.notes_delete_dialog_body,
) {
    OffhandDialog(
        title = stringResource(titleRes),
        body = stringResource(bodyRes),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.notes_delete_dialog_cancel),
        onConfirm = onDismiss,
        destructiveLabel = stringResource(R.string.notes_delete_dialog_confirm),
        onDestructive = onDelete,
    )
}

@Composable
internal fun DeleteFolderDialog(onDelete: () -> Unit, onDismiss: () -> Unit) {
    DeleteNoteDialog(
        onDelete = onDelete,
        onDismiss = onDismiss,
        titleRes = R.string.notes_folder_delete_dialog_title,
        bodyRes = R.string.notes_folder_delete_dialog_body,
    )
}

@Composable
internal fun RetranscribeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    OffhandDialog(
        title = stringResource(R.string.notes_retranscribe_dialog_title),
        body = stringResource(R.string.notes_retranscribe_dialog_body),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.notes_delete_dialog_cancel),
        onConfirm = onDismiss,
        destructiveLabel = stringResource(R.string.notes_retranscribe_description),
        onDestructive = onConfirm,
    )
}

@Composable
internal fun ImportMessageDialog(message: ImportMessageUi, onDismiss: () -> Unit) {
    val body = when (message) {
        ImportMessageUi.UNSUPPORTED -> R.string.notes_import_error_unsupported
        ImportMessageUi.TOO_LONG -> R.string.notes_import_error_too_long
        ImportMessageUi.UNREADABLE -> R.string.notes_import_error_unreadable
    }
    NoticeDialog(titleRes = R.string.notes_import_error_title, bodyRes = body, onDismiss = onDismiss)
}

@Composable
internal fun NoticeDialog(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    onDismiss: () -> Unit,
    @StringRes dismissRes: Int = R.string.notes_import_error_dismiss,
) {
    OffhandDialog(
        title = stringResource(titleRes),
        body = stringResource(bodyRes),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(dismissRes),
        onConfirm = onDismiss,
    )
}
