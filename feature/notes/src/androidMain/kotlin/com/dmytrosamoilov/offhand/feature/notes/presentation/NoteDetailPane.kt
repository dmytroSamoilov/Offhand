package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.data.domain.analytics.NoteSection
import com.dmytrosamoilov.offhand.core.designsystem.component.BarIconButton
import com.dmytrosamoilov.offhand.core.designsystem.component.Hairline
import com.dmytrosamoilov.offhand.core.designsystem.component.MetaText
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.Player
import com.dmytrosamoilov.offhand.core.designsystem.component.PrimaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.RoundIconButton
import com.dmytrosamoilov.offhand.core.designsystem.component.TabItem
import com.dmytrosamoilov.offhand.core.designsystem.component.TopBar
import com.dmytrosamoilov.offhand.core.designsystem.component.UnderlineTabs
import com.dmytrosamoilov.offhand.core.designsystem.haptics.haptics
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.core.ui.rememberSensitiveClipboard
import com.dmytrosamoilov.offhand.feature.notes.R

@Immutable
internal data class NoteDetailTabsUi(
    val visible: List<NoteDetailTabUi>,
    val selected: NoteDetailTabUi?,
    val isSummaryReadyMarked: Boolean,
    val onSelected: (NoteDetailTabUi) -> Unit,
)

@Composable
internal fun NoteDetailPane(state: NotesUiState, viewModel: NotesViewModel, showsBack: Boolean) {
    val selected = state.selected
    val editor = state.editor
    when {
        selected == null -> EmptyDetailPlaceholder()
        editor != null -> NoteEditor(
            editor = editor,
            onTitleChanged = viewModel::onEditorTitleChanged,
            onTranscriptChanged = viewModel::onEditorTranscriptChanged,
            onSave = viewModel::onEditSaved,
            onCancel = viewModel::onEditCancelled,
        )
        else -> NoteDetail(state = state, viewModel = viewModel, note = selected, showsBack = showsBack)
    }
}

@Composable
private fun EmptyDetailPlaceholder() {
    Box(modifier = Modifier.fillMaxSize().background(OffhandTheme.colors.ground), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.notes_select_prompt),
            style = OffhandTheme.type.body,
            color = OffhandTheme.colors.ink2,
        )
    }
}

@Composable
private fun NoteDetail(state: NotesUiState, viewModel: NotesViewModel, note: NoteDetailUi, showsBack: Boolean) {
    val tabs = NoteDetailTabsUi(
        visible = state.visibleDetailTabs,
        selected = state.shownDetailTab,
        isSummaryReadyMarked = state.isSummaryReadyMarked,
        onSelected = viewModel::onDetailTabSelected,
    )
    Column(modifier = Modifier.fillMaxSize().background(OffhandTheme.colors.ground)) {
        TopBar(
            leading = { if (showsBack) BackButton(onBack = viewModel::onDetailClosed) },
            trailing = { NoteOverflowMenu(note = note, state = state, viewModel = viewModel) },
        )
        NoteDetailHeader(note = note, playback = state.playback, tabs = tabs, viewModel = viewModel)
        NoteDetailBody(state = state, viewModel = viewModel, note = note, tabs = tabs, modifier = Modifier.weight(1f))
        NoteDetailFooter(note = note, tab = tabs.selected, onShare = viewModel::onShareRequested, onCopied = viewModel::onNoteCopied)
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    BarIconButton(icon = OffhandIcons.Back, contentDescription = stringResource(R.string.notes_back_description), onClick = onBack)
}

@Composable
private fun NoteDetailHeader(note: NoteDetailUi, playback: AudioPlaybackUi, tabs: NoteDetailTabsUi, viewModel: NotesViewModel) {
    val hasPlayer = note.hasAudio && playback.isAvailable
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(
            text = note.detailTitle(),
            style = OffhandTheme.type.title,
            color = OffhandTheme.colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        MetaText(
            text = listOfNotNull(note.createdAt, playback.durationText.takeIf { hasPlayer }, note.folderName)
                .joinToString(separator = " ${stringResource(R.string.notes_metadata_separator)} "),
            modifier = Modifier.padding(top = 8.dp),
        )
        if (hasPlayer) NotePlayer(playback = playback, noteId = note.id, viewModel = viewModel)
        if (tabs.visible.size > 1) NoteDetailTabs(tabs = tabs)
    }
}

@Composable
private fun NotePlayer(playback: AudioPlaybackUi, noteId: Long, viewModel: NotesViewModel) {
    val haptics = haptics()
    Player(
        isPlaying = playback.isPlaying,
        progress = playback.progress,
        elapsedLabel = if (playback.isPlaying || playback.progress > 0f) playback.positionText else playback.durationText,
        playDescription = stringResource(R.string.notes_play_description),
        pauseDescription = stringResource(R.string.notes_pause_description),
        seekDescription = stringResource(R.string.notes_seek_description),
        waveSeed = noteId,
        onToggle = {
            haptics.confirm()
            viewModel.onPlayPauseClicked()
        },
        onSeek = viewModel::onSeekRequested,
        modifier = Modifier.padding(top = 22.dp),
    )
}

@Composable
private fun NoteDetailTabs(tabs: NoteDetailTabsUi) {
    val haptics = haptics()
    val readyDescription = stringResource(R.string.notes_summary_ready_description)
    val markedIndex = tabs.visible.indexOf(NoteDetailTabUi.SUMMARY).takeIf { tabs.isSummaryReadyMarked }
    UnderlineTabs(
        tabs = tabs.visible.mapIndexed { index, tab ->
            TabItem(label = stringResource(tab.labelRes()), dotDescription = readyDescription.takeIf { index == markedIndex })
        },
        selectedIndex = tabs.visible.indexOf(tabs.selected),
        onSelect = { index ->
            haptics.tick()
            tabs.onSelected(tabs.visible[index])
        },
        tabModifier = { index ->
            if (index == markedIndex) Modifier.semantics { contentDescription = readyDescription } else Modifier
        },
        modifier = Modifier.padding(top = 18.dp),
    )
}

private fun NoteDetailTabUi.labelRes(): Int = when (this) {
    NoteDetailTabUi.SUMMARY -> R.string.notes_summary_heading
    NoteDetailTabUi.TRANSCRIPT -> R.string.notes_transcript_heading
}

@Composable
private fun NoteOverflowMenu(note: NoteDetailUi, state: NotesUiState, viewModel: NotesViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        BarIconButton(
            icon = OffhandIcons.More,
            contentDescription = stringResource(R.string.notes_more_actions_description),
            onClick = { expanded = true },
        )
        NotesMenu(expanded = expanded, onDismiss = { expanded = false }) {
            noteMenuActions(note, state, viewModel).forEach { action ->
                NotesMenuItem(
                    text = stringResource(action.textRes),
                    icon = action.icon,
                    isDanger = action.isDanger,
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}

private data class NoteMenuAction(
    @param:StringRes val textRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isDanger: Boolean = false,
)

private fun noteMenuActions(note: NoteDetailUi, state: NotesUiState, viewModel: NotesViewModel): List<NoteMenuAction> {
    val delete = NoteMenuAction(R.string.notes_delete_description, OffhandIcons.Trash, { viewModel.onDeleteRequested(note.id) }, isDanger = true)
    if (note.status != NoteStatusUi.READY) return listOf(delete)
    return listOfNotNull(
        NoteMenuAction(R.string.notes_edit_description, OffhandIcons.Edit, viewModel::onEditStarted),
        NoteMenuAction(R.string.notes_move_to_folder, OffhandIcons.Folder, viewModel::onMoveToFolderRequested),
        NoteMenuAction(R.string.notes_preset_description, OffhandIcons.Sparkle, viewModel::onPresetSheetRequested)
            .takeIf { note.transcript.isNotBlank() && state.isSummaryAvailable },
        NoteMenuAction(R.string.notes_retranscribe_description, OffhandIcons.Mic, viewModel::onRetranscribeRequested)
            .takeIf { note.hasAudio && state.isRetranscribeAvailable },
        delete,
    )
}

private data class CopyTarget(
    val text: String,
    @param:StringRes val clipboardLabelRes: Int,
    @param:StringRes val buttonRes: Int,
    val section: NoteSection,
)

private fun NoteDetailUi.copyTarget(tab: NoteDetailTabUi?): CopyTarget? = when (tab) {
    NoteDetailTabUi.SUMMARY -> CopyTarget(body, R.string.notes_summary_heading, R.string.notes_copy_summary_description, NoteSection.OVERVIEW)
    NoteDetailTabUi.TRANSCRIPT -> CopyTarget(transcript, R.string.notes_transcript_heading, R.string.notes_copy_transcript_description, NoteSection.TRANSCRIPT)
    null -> null
}?.takeIf { it.text.isNotBlank() }

@Composable
private fun NoteDetailFooter(note: NoteDetailUi, tab: NoteDetailTabUi?, onShare: () -> Unit, onCopied: (NoteSection) -> Unit) {
    if (note.status != NoteStatusUi.READY) {
        Spacer(modifier = Modifier.navigationBarsPadding())
        return
    }
    Column(modifier = Modifier.fillMaxWidth().background(OffhandTheme.colors.ground)) {
        Hairline()
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        ) {
            note.copyTarget(tab)?.let { target -> CopyButton(target = target, onCopied = onCopied, modifier = Modifier.weight(1f)) }
            RoundIconButton(icon = OffhandIcons.Share, contentDescription = stringResource(R.string.notes_share_description), onClick = onShare)
        }
    }
}

@Composable
private fun CopyButton(target: CopyTarget, onCopied: (NoteSection) -> Unit, modifier: Modifier = Modifier) {
    val clipboard = rememberSensitiveClipboard()
    val haptics = haptics()
    val clipboardLabel = stringResource(target.clipboardLabelRes)
    val copiedMessage = stringResource(R.string.notes_copied)
    PrimaryButton(
        text = stringResource(target.buttonRes),
        icon = OffhandIcons.Copy,
        onClick = {
            haptics.confirm()
            clipboard.copy(clipboardLabel, target.text, copiedMessage)
            onCopied(target.section)
        },
        modifier = modifier,
    )
}

@Composable
private fun NoteDetailUi.detailTitle(): String = when (status) {
    NoteStatusUi.PROCESSING -> title.ifBlank { stringResource(R.string.notes_processing_title) }
    NoteStatusUi.FAILED, NoteStatusUi.INTERRUPTED -> title.ifBlank { stringResource(R.string.notes_recording_fallback_title) }
    NoteStatusUi.READY -> title
}
