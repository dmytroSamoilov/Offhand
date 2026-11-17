package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.component.Hairline
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.PlainTextButton
import com.dmytrosamoilov.offhand.core.designsystem.component.PrimaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.SearchField
import com.dmytrosamoilov.offhand.core.designsystem.component.TextButtonTone
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.notes.R

@Immutable
internal data class NoteSelectionUi(
    val selectedIds: Set<Long>,
    val onToggle: (Long) -> Unit,
    val onClear: () -> Unit,
    val onMove: () -> Unit,
    val onDelete: () -> Unit,
) {
    val isActive: Boolean get() = selectedIds.isNotEmpty()
}

@Composable
internal fun rememberNoteSelection(state: NotesUiState, viewModel: NotesViewModel): NoteSelectionUi =
    remember(state.selectedNoteIds, viewModel) {
        NoteSelectionUi(
            selectedIds = state.selectedNoteIds,
            onToggle = viewModel::onNoteSelectionToggled,
            onClear = viewModel::onNoteSelectionCleared,
            onMove = { viewModel.onBulkActionRequested(BulkNoteActionUi.MOVE) },
            onDelete = { viewModel.onBulkActionRequested(BulkNoteActionUi.DELETE) },
        )
    }

@Composable
internal fun SelectionHeader(selection: NoteSelectionUi) {
    val cancelDescription = stringResource(R.string.notes_selection_cancel)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.notes_selection_count, selection.selectedIds.size),
            style = OffhandTheme.type.title,
            color = OffhandTheme.colors.ink,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        PlainTextButton(
            text = stringResource(R.string.notes_delete_dialog_cancel),
            onClick = selection.onClear,
            tone = TextButtonTone.Ink,
            modifier = Modifier.semantics { contentDescription = cancelDescription },
        )
    }
}

@Composable
internal fun SelectionFooter(selection: NoteSelectionUi) {
    Column(modifier = Modifier.fillMaxWidth().background(OffhandTheme.colors.ground)) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(
                text = stringResource(R.string.notes_move_to_folder),
                icon = OffhandIcons.Folder,
                onClick = selection.onMove,
                modifier = Modifier.weight(1f),
            )
            PlainTextButton(
                text = stringResource(R.string.notes_delete_dialog_confirm),
                onClick = selection.onDelete,
                tone = TextButtonTone.Danger,
            )
        }
    }
}

@Composable
internal fun SearchHeader(query: String, onQueryChanged: (String) -> Unit, onCancel: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val currentQuery by rememberUpdatedState(query)
    LaunchedEffect(Unit) { if (query.isEmpty()) focusRequester.requestFocus() }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SearchField(
                query = query,
                onQueryChange = onQueryChanged,
                placeholder = stringResource(R.string.notes_search_placeholder),
                clearDescription = stringResource(R.string.notes_search_clear_description),
                onSearch = { keyboard?.hide() },
                modifier = Modifier.weight(1f).focusRequester(focusRequester).onFocusChanged { focus ->
                    if (isFocused && !focus.isFocused && currentQuery.isEmpty()) onCancel()
                    isFocused = focus.isFocused
                },
            )
            AnimatedVisibility(visible = isFocused, enter = fadeIn(), exit = fadeOut()) {
                CancelSearchButton(onClick = {
                    focusManager.clearFocus()
                    onCancel()
                })
            }
        }
        Hairline()
    }
}

@Composable
private fun CancelSearchButton(onClick: () -> Unit) {
    val description = stringResource(R.string.notes_search_cancel_description)
    PlainTextButton(
        text = stringResource(R.string.notes_delete_dialog_cancel),
        onClick = onClick,
        tone = TextButtonTone.Ink,
        modifier = Modifier.semantics { contentDescription = description },
    )
}
