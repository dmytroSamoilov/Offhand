package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dmytrosamoilov.offhand.core.ui.component.NoteStyleChoice
import com.dmytrosamoilov.offhand.core.ui.component.NoteStylePickerSheet
import com.dmytrosamoilov.offhand.feature.notes.R

@Composable
internal fun NotesOverlays(
    state: NotesUiState,
    viewModel: NotesViewModel,
    onNewStyle: (StyleEditorOrigin) -> Unit,
) {
    NoteDialogs(state = state, viewModel = viewModel)
    FolderOverlays(state = state, viewModel = viewModel, onNewStyle = onNewStyle)
    MoveSheets(state = state, viewModel = viewModel)
    if (state.isShareDialogVisible) {
        ShareNoteSheet(
            hasAudio = state.selected?.hasAudio == true,
            isDocumentExportUnlocked = state.isDocumentExportUnlocked,
            onShare = viewModel::onShareConfirmed,
            onSaveToDevice = viewModel::onSaveToDeviceConfirmed,
            onDismiss = viewModel::onShareDismissed,
        )
    }
    CalendarEventLauncher(event = state.pendingCalendarEvent, onLaunched = viewModel::onCalendarEventLaunched)
    NoteStyleSheet(state = state, viewModel = viewModel, onNewStyle = onNewStyle)
}

@Composable
private fun NoteDialogs(state: NotesUiState, viewModel: NotesViewModel) {
    if (state.pendingDeleteNoteId != null) {
        DeleteNoteDialog(onDelete = viewModel::onDeleteConfirmed, onDismiss = viewModel::onDeleteDismissed)
    }
    if (state.bulkAction == BulkNoteActionUi.DELETE) {
        DeleteNoteDialog(
            onDelete = viewModel::onBulkDeleteConfirmed,
            onDismiss = viewModel::onBulkActionDismissed,
            titleRes = R.string.notes_bulk_delete_title,
            bodyRes = R.string.notes_bulk_delete_body,
        )
    }
    if (state.isRetranscribeConfirmationVisible) {
        RetranscribeDialog(onConfirm = viewModel::onRetranscribeConfirmed, onDismiss = viewModel::onRetranscribeDismissed)
    }
    state.importMessage?.let { message ->
        ImportMessageDialog(message = message, onDismiss = viewModel::onImportMessageDismissed)
    }
}

@Composable
private fun FolderOverlays(
    state: NotesUiState,
    viewModel: NotesViewModel,
    onNewStyle: (StyleEditorOrigin) -> Unit,
) {
    state.folderEditor?.let { editor ->
        FolderEditorDialog(
            editor = editor,
            onNameChanged = viewModel::onFolderNameChanged,
            onConfirm = viewModel::onFolderEditorConfirmed,
            onDismiss = viewModel::onFolderEditorDismissed,
        )
    }
    if (state.pendingDeleteFolderId != null) {
        DeleteFolderDialog(onDelete = viewModel::onDeleteFolderConfirmed, onDismiss = viewModel::onDeleteFolderDismissed)
    }
    state.folderStylePicker?.let { picker ->
        NoteStylePickerSheet(
            title = stringResource(R.string.notes_folder_style_title, picker.folderName),
            body = stringResource(R.string.notes_folder_style_body),
            selected = picker.style,
            customStyles = state.customStyles.toChoices(),
            isProStylesUnlocked = state.isFolderStylesUnlocked,
            onSelected = viewModel::onFolderStyleSelected,
            onDismiss = viewModel::onFolderStyleDismissed,
            onDefaultSelected = { viewModel.onFolderStyleSelected(null) },
            onCreateStyle = {
                viewModel.onFolderStyleDismissed()
                onNewStyle(StyleEditorOrigin.Folder(picker.folderId))
            },
        )
    }
}

@Composable
private fun MoveSheets(state: NotesUiState, viewModel: NotesViewModel) {
    val target = state.moveToFolder
    val isBulk = state.bulkAction == BulkNoteActionUi.MOVE
    if (target == null && !isBulk) return
    MoveToFolderSheet(
        folders = state.folders,
        customStyles = state.customStyles.toChoices(),
        bulkCount = state.selectedNoteIds.size.takeIf { isBulk },
        currentFolderId = target?.currentFolderId,
        isFolderLimitReached = state.isFolderLimitReached,
        onMove = if (isBulk) viewModel::onBulkMoveConfirmed else viewModel::onMoveToFolder,
        onNewFolder = viewModel::onNewFolderRequested,
        onDismiss = if (isBulk) viewModel::onBulkActionDismissed else viewModel::onMoveToFolderDismissed,
    )
}

@Composable
private fun NoteStyleSheet(
    state: NotesUiState,
    viewModel: NotesViewModel,
    onNewStyle: (StyleEditorOrigin) -> Unit,
) {
    val selectedStyle = state.selected?.style
    if (!state.isPresetSheetVisible || selectedStyle == null) return
    NoteStylePickerSheet(
        title = stringResource(R.string.notes_preset_description),
        body = stringResource(R.string.notes_preset_sheet_body),
        selected = selectedStyle,
        customStyles = state.customStyles.toChoices(),
        isProStylesUnlocked = state.isProStylesUnlocked,
        onSelected = viewModel::onStyleSelected,
        onDismiss = viewModel::onPresetSheetDismissed,
        onCreateStyle = {
            viewModel.onPresetSheetDismissed()
            onNewStyle(StyleEditorOrigin.Note)
        },
    )
}

internal fun List<NoteStyleOptionUi>.toChoices(): List<NoteStyleChoice> =
    map { NoteStyleChoice(id = it.id, name = it.name, description = it.description) }
