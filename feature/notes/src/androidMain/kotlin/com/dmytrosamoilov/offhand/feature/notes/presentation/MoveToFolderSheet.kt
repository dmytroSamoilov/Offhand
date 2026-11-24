package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.component.ChoiceRow
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandSheet
import com.dmytrosamoilov.offhand.core.designsystem.component.PrimaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.ProBadge
import com.dmytrosamoilov.offhand.core.designsystem.component.SecondaryButton
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.core.ui.component.NoteStyleChoice
import com.dmytrosamoilov.offhand.core.ui.component.label
import com.dmytrosamoilov.offhand.feature.notes.R

private data class FolderChoice(val folderId: Long?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoveToFolderSheet(
    folders: List<FolderUi>,
    customStyles: List<NoteStyleChoice>,
    bulkCount: Int?,
    currentFolderId: Long?,
    isFolderLimitReached: Boolean,
    rewritesNotesMovedTo: (Long?) -> Boolean,
    showsFolderStyles: Boolean,
    onMove: (Long?) -> Unit,
    onNewFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    var choice by remember { mutableStateOf(FolderChoice(currentFolderId).takeIf { bulkCount == null }) }
    OffhandSheet(onDismissRequest = onDismiss, title = moveTitle(bulkCount)) {
        Column(modifier = Modifier.heightIn(max = MAX_LIST_HEIGHT).verticalScroll(rememberScrollState())) {
            ChoiceRow(
                title = stringResource(R.string.notes_folder_none),
                isSelected = choice == FolderChoice(null),
                onClick = { choice = FolderChoice(null) },
            )
            folders.forEach { folder ->
                ChoiceRow(
                    title = folder.name,
                    subtitle = folder.style?.takeIf { showsFolderStyles }?.label(customStyles),
                    hint = folder.noteCount.toString(),
                    isSelected = choice == FolderChoice(folder.id),
                    onClick = { choice = FolderChoice(folder.id) },
                )
            }
            NewFolderRow(
                showProBadge = isFolderLimitReached,
                onClick = {
                    if (isFolderLimitReached) onDismiss()
                    onNewFolder()
                },
            )
        }
        choice?.folderId
            ?.takeIf { it != currentFolderId && rewritesNotesMovedTo(it) }
            ?.let { folderId -> folders.firstOrNull { it.id == folderId } }
            ?.let { folder -> RewriteCaption(folder = folder, customStyles = customStyles, noteCount = bulkCount ?: 1) }
        MoveActions(
            isMoveEnabled = choice != null,
            onCancel = onDismiss,
            onMove = { choice?.let { onMove(it.folderId) } },
        )
    }
}

@Composable
private fun moveTitle(bulkCount: Int?): String =
    if (bulkCount == null) {
        stringResource(R.string.notes_move_to_folder)
    } else {
        pluralStringResource(R.plurals.notes_move_notes_title, bulkCount, bulkCount)
    }

@Composable
private fun RewriteCaption(folder: FolderUi, customStyles: List<NoteStyleChoice>, noteCount: Int) {
    val style = folder.style ?: return
    Text(
        text = pluralStringResource(R.plurals.notes_move_rewrite_caption, noteCount, folder.name, style.label(customStyles), noteCount),
        style = OffhandTheme.type.caption,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun NewFolderRow(showProBadge: Boolean, onClick: () -> Unit) {
    val colors = OffhandTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(OffhandIcons.Plus, contentDescription = null, tint = colors.ink, modifier = Modifier.size(22.dp))
        Text(
            text = stringResource(R.string.notes_folder_new),
            style = OffhandTheme.type.body,
            fontWeight = FontWeight.Medium,
            color = colors.ink,
        )
        if (showProBadge) ProBadge()
    }
}

@Composable
private fun MoveActions(isMoveEnabled: Boolean, onCancel: () -> Unit, onMove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SecondaryButton(
            text = stringResource(R.string.notes_delete_dialog_cancel),
            onClick = onCancel,
            modifier = Modifier.weight(1f),
        )
        PrimaryButton(
            text = stringResource(R.string.notes_move_confirm),
            onClick = onMove,
            enabled = isMoveEnabled,
            modifier = Modifier.weight(1f),
        )
    }
}

private val MAX_LIST_HEIGHT = 420.dp
