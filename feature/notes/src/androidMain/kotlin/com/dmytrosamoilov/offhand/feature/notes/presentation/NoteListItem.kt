package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.component.ChoiceMark
import com.dmytrosamoilov.offhand.core.designsystem.component.Hairline
import com.dmytrosamoilov.offhand.core.designsystem.component.NoteRow
import com.dmytrosamoilov.offhand.core.designsystem.component.NoteRowState
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.haptics.haptics
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.notes.R

@Immutable
internal data class NoteRowContext(
    val selection: NoteSelectionUi,
    val writesSummary: Boolean,
    val openNoteId: Long?,
    val isSearching: Boolean,
    val onOpen: (Long) -> Unit,
)

@Composable
internal fun NoteListItem(
    note: NoteCardUi,
    isWaitingForAi: Boolean,
    progressPercent: Int?,
    context: NoteRowContext,
    onDeleteRequested: () -> Unit,
    onMoveRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOpen = note.id == context.openNoteId
    SwipeableRow(
        onDeleteRequested = onDeleteRequested,
        onMoveRequested = onMoveRequested,
        isSwipeEnabled = !context.selection.isActive,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.background(OffhandTheme.colors.ground)) {
            SelectableNoteRow(note = note, isWaitingForAi = isWaitingForAi, progressPercent = progressPercent, context = context, isOpen = isOpen)
            if (!isOpen) Hairline(modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}

@Composable
private fun SelectableNoteRow(
    note: NoteCardUi,
    isWaitingForAi: Boolean,
    progressPercent: Int?,
    context: NoteRowContext,
    isOpen: Boolean,
) {
    val haptics = haptics()
    val selection = context.selection
    val isSelected = note.id in selection.selectedIds
    NoteRow(
        title = note.rowTitle().highlighted(note.titleHighlights),
        state = note.rowState(isWaitingForAi = isWaitingForAi, writesSummary = context.writesSummary, progressPercent = progressPercent),
        meta = note.meta(showsDay = context.isSearching),
        previewLines = if (context.isSearching) SEARCH_PREVIEW_LINES else 1,
        waveSeed = note.id,
        showWave = !selection.isActive,
        leading = { if (selection.isActive) ChoiceMark(isSelected = isSelected, size = 24.dp) },
        modifier = Modifier
            .rowSurface(isSelected = isSelected, isOpen = isOpen)
            .combinedClickable(
                onClick = { if (selection.isActive) selection.onToggle(note.id) else context.onOpen(note.id) },
                onLongClick = {
                    haptics.confirm()
                    selection.onToggle(note.id)
                },
                onLongClickLabel = stringResource(R.string.notes_select_action),
            )
            .semantics { selected = isSelected }
            .padding(horizontal = if (isOpen) 12.dp else 24.dp),
    )
}

@Composable
private fun Modifier.rowSurface(isSelected: Boolean, isOpen: Boolean): Modifier {
    val colors = OffhandTheme.colors
    val shape = RoundedCornerShape(14.dp)
    return when {
        isSelected -> background(colors.accentSoft)
        isOpen -> padding(horizontal = 12.dp).shadow(1.dp, shape).background(colors.surface, shape)
        else -> this
    }
}

@Composable
private fun SwipeableRow(
    onDeleteRequested: () -> Unit,
    onMoveRequested: () -> Unit,
    isSwipeEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentOnDeleteRequested by rememberUpdatedState(onDeleteRequested)
    val currentOnMoveRequested by rememberUpdatedState(onMoveRequested)
    val haptics = haptics()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> currentOnDeleteRequested()
                SwipeToDismissBoxValue.StartToEnd -> currentOnMoveRequested()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            if (value != SwipeToDismissBoxValue.Settled) haptics.gestureEnd()
            value == SwipeToDismissBoxValue.Settled
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground(direction = dismissState.dismissDirection) },
        modifier = modifier,
        enableDismissFromStartToEnd = isSwipeEnabled,
        enableDismissFromEndToStart = isSwipeEnabled,
    ) {
        content()
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val colors = OffhandTheme.colors
    val isMove = direction == SwipeToDismissBoxValue.StartToEnd
    val isDelete = direction == SwipeToDismissBoxValue.EndToStart
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDelete) colors.danger else if (isMove) colors.well else Color.Transparent)
            .padding(horizontal = 28.dp),
        contentAlignment = if (isMove) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = if (isMove) OffhandIcons.Folder else OffhandIcons.Trash,
            contentDescription = null,
            tint = if (isDelete) colors.onInk else colors.ink,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun NoteCardUi.rowTitle(): String = when (status) {
    NoteStatusUi.PROCESSING -> title.ifBlank { stringResource(R.string.notes_processing_title) }
    NoteStatusUi.FAILED, NoteStatusUi.INTERRUPTED -> title.ifBlank { stringResource(R.string.notes_recording_fallback_title) }
    NoteStatusUi.READY -> title
}

@Composable
private fun NoteCardUi.rowState(isWaitingForAi: Boolean, writesSummary: Boolean, progressPercent: Int?): NoteRowState = when (status) {
    NoteStatusUi.PROCESSING if isWaitingForAi -> NoteRowState.Attention(stringResource(R.string.notes_processing_waiting_preview))
    NoteStatusUi.PROCESSING -> NoteRowState.Processing(
        text = stringResource(if (writesSummary) R.string.notes_processing_preview else R.string.notes_processing_preview_transcript),
        progress = progressPercent?.let { it / PERCENT },
    )
    NoteStatusUi.FAILED -> NoteRowState.Attention(stringResource(R.string.notes_failed_description))
    NoteStatusUi.INTERRUPTED -> NoteRowState.Attention(stringResource(R.string.notes_interrupted_preview))
    NoteStatusUi.READY -> NoteRowState.Ready(preview.takeIf { it.isNotBlank() }?.highlighted(previewHighlights))
}

@Composable
private fun NoteCardUi.meta(showsDay: Boolean): String {
    val parts = if (showsDay) listOfNotNull("${dayLabel.text()} $time", matchLabel(), folderName) else listOfNotNull(time, durationText, folderName)
    return parts.joinToString(separator = " ${stringResource(R.string.notes_metadata_separator)} ")
}

@Composable
private fun NoteCardUi.matchLabel(): String? = when (matchLocation) {
    null -> null
    MatchLocationUi.TITLE -> stringResource(R.string.notes_match_title)
    MatchLocationUi.SUMMARY -> stringResource(R.string.notes_match_summary)
    MatchLocationUi.TRANSCRIPT -> matchTime?.let { stringResource(R.string.notes_match_transcript_at, it) }
        ?: stringResource(R.string.notes_match_transcript)
}

@Composable
private fun String.highlighted(ranges: List<TextRangeUi>): AnnotatedString {
    val style = SpanStyle(background = OffhandTheme.colors.accentSoft)
    return buildAnnotatedString {
        append(this@highlighted)
        ranges.forEach { range -> addStyle(style, range.start, range.end.coerceAtMost(length)) }
    }
}

private const val PERCENT = 100f

private const val SEARCH_PREVIEW_LINES = 2
