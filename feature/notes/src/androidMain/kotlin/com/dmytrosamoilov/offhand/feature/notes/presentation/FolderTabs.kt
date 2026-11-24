package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.ProBadge
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandMotion
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.core.ui.component.label
import com.dmytrosamoilov.offhand.feature.notes.R

@Composable
internal fun FolderTabs(state: NotesUiState, viewModel: NotesViewModel) {
    val listState = rememberLazyListState()
    val drag = remember { FolderDragState() }
    val currentFolders by rememberUpdatedState(state.folders)
    val spacingPx = with(LocalDensity.current) { TAB_SPACING.roundToPx() }
    val hairline = OffhandTheme.colors.hairline
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(TAB_SPACING),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = OffhandSpacing.screen),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .drawBehind { drawLine(hairline, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx()) },
    ) {
        item(key = ALL_NOTES_KEY) {
            FolderTab(
                label = stringResource(if (state.isUnsortedShown) R.string.notes_folder_unsorted else R.string.notes_folder_all),
                isSelected = state.selectedFolderId == null,
                onClick = { viewModel.onFolderSelected(null) },
            )
        }
        items(state.folders, key = { it.id }) { folder ->
            FolderTabWithMenu(
                folder = folder,
                state = state,
                viewModel = viewModel,
                modifier = Modifier
                    .animateItem()
                    .zIndex(if (drag.draggingId == folder.id) 1f else 0f)
                    .graphicsLayer { translationX = if (drag.draggingId == folder.id) drag.offset else 0f }
                    .pointerInput(folder.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { drag.start(folder.id) },
                            onDragEnd = { drag.end() },
                            onDragCancel = { drag.end() },
                            onDrag = { change, amount ->
                                change.consume()
                                drag.offset += amount.x
                                drag.swapIfCrossed(listState, currentFolders, folder.id, spacingPx, viewModel::onFolderMoved)
                            },
                        )
                    },
            )
        }
        item(key = NEW_FOLDER_KEY) { NewFolderButton(showProBadge = state.isFolderLimitReached, onClick = viewModel::onNewFolderRequested) }
    }
}

@Composable
private fun FolderTabWithMenu(folder: FolderUi, state: NotesUiState, viewModel: NotesViewModel, modifier: Modifier) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val isSelected = folder.id == state.selectedFolderId
    Box(modifier = modifier) {
        FolderTab(
            label = folder.name,
            isSelected = isSelected,
            showsMenuMark = isSelected,
            onClick = { if (isSelected) isMenuExpanded = true else viewModel.onFolderSelected(folder.id) },
        )
        NotesMenu(expanded = isMenuExpanded, onDismiss = { isMenuExpanded = false }) {
            FolderMenuItems(folder = folder, state = state, viewModel = viewModel, onDismiss = { isMenuExpanded = false })
        }
    }
}

@Composable
private fun FolderMenuItems(folder: FolderUi, state: NotesUiState, viewModel: NotesViewModel, onDismiss: () -> Unit) {
    if (state.isSummaryAvailable) FolderStyleMenuItem(folder = folder, state = state, viewModel = viewModel, onDismiss = onDismiss)
    NotesMenuItem(text = stringResource(R.string.notes_folder_rename), icon = OffhandIcons.Edit, onClick = {
        onDismiss()
        viewModel.onRenameFolderRequested(folder.id)
    })
    NotesMenuItem(text = stringResource(R.string.notes_folder_delete), icon = OffhandIcons.Trash, isDanger = true, onClick = {
        onDismiss()
        viewModel.onDeleteFolderRequested(folder.id)
    })
}

@Composable
private fun FolderStyleMenuItem(folder: FolderUi, state: NotesUiState, viewModel: NotesViewModel, onDismiss: () -> Unit) {
    NotesMenuItem(
        text = stringResource(R.string.notes_folder_style),
        subtitle = folder.style?.label(state.customStyles.toChoices()) ?: stringResource(R.string.notes_folder_style_default),
        showProBadge = !state.isFolderStylesUnlocked,
        icon = OffhandIcons.Sparkle,
        onClick = {
            onDismiss()
            viewModel.onFolderStyleRequested(folder.id)
        },
    )
}

@Composable
private fun FolderTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    showsMenuMark: Boolean = false,
) {
    val colors = OffhandTheme.colors
    val textColor by animateColorAsState(if (isSelected) colors.ink else colors.ink2, OffhandMotion.state(), label = "folder-tab")
    val accent = colors.accent
    Row(
        modifier = Modifier
            .height(OffhandSpacing.touchTarget)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .drawBehind {
                if (isSelected) {
                    val stroke = 2.dp.toPx()
                    drawLine(accent, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = OffhandTheme.type.tab,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
        )
        if (showsMenuMark) FolderMenuMark()
    }
}

@Composable
private fun FolderMenuMark() {
    Icon(
        imageVector = OffhandIcons.ChevronDown,
        contentDescription = stringResource(R.string.notes_folder_options_description),
        tint = OffhandTheme.colors.ink2,
        modifier = Modifier.size(14.dp),
    )
}

@Composable
private fun NewFolderButton(showProBadge: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.notes_folder_new)
    Row(
        modifier = Modifier
            .height(OffhandSpacing.touchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(OffhandIcons.Plus, contentDescription = null, tint = OffhandTheme.colors.ink2, modifier = Modifier.size(18.dp))
        if (showProBadge) ProBadge()
    }
}

private class FolderDragState {
    var draggingId: Long? by mutableStateOf(null)
    var offset: Float by mutableFloatStateOf(0f)

    fun start(id: Long) {
        draggingId = id
        offset = 0f
    }

    fun end() {
        draggingId = null
        offset = 0f
    }

    fun swapIfCrossed(
        listState: LazyListState,
        folders: List<FolderUi>,
        draggedId: Long,
        spacingPx: Int,
        onMoved: (Int, Int) -> Unit,
    ) {
        val items = listState.layoutInfo.visibleItemsInfo
        val dragged = items.firstOrNull { it.key == draggedId } ?: return
        val centre = dragged.offset + offset + dragged.size / 2f
        val target = items.firstOrNull { item ->
            item.key != draggedId && item.key is Long && centre >= item.offset && centre <= item.offset + item.size
        } ?: return
        val from = folders.indexOfFirst { it.id == draggedId }
        val to = folders.indexOfFirst { it.id == target.key }
        if (from < 0 || to < 0 || from == to) return
        offset += (target.size + spacingPx) * if (to > from) -1 else 1
        onMoved(from, to)
    }
}

private const val ALL_NOTES_KEY = "all-notes"
private const val NEW_FOLDER_KEY = "new-folder"
private val TAB_SPACING = 22.dp
