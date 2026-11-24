package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.component.ChoiceRow
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandSheet
import com.dmytrosamoilov.offhand.core.designsystem.component.PrimaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.SecondaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.SegmentedControl
import com.dmytrosamoilov.offhand.core.designsystem.haptics.haptics
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.notes.R
import com.dmytrosamoilov.offhand.feature.notes.domain.export.NoteExportFormat

private enum class ShareTabUi { TEXT, AUDIO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareNoteSheet(
    hasAudio: Boolean,
    isDocumentExportUnlocked: Boolean,
    onShare: (NoteExportFormat?, Boolean) -> Unit,
    onSaveToDevice: (NoteExportFormat?, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by remember { mutableStateOf(ShareTabUi.TEXT) }
    var noteFormat by remember { mutableStateOf(NoteExportFormat.TEXT) }
    val isText = tab == ShareTabUi.TEXT
    OffhandSheet(onDismissRequest = onDismiss, title = stringResource(R.string.notes_share_description)) {
        if (hasAudio) ShareTabs(selected = tab, onSelected = { tab = it })
        when (tab) {
            ShareTabUi.TEXT -> ShareFormatOptions(
                selected = noteFormat,
                isDocumentExportUnlocked = isDocumentExportUnlocked,
                onSelected = { noteFormat = it },
            )
            ShareTabUi.AUDIO -> ShareAudioDetails()
        }
        Text(
            text = stringResource(R.string.notes_share_privacy),
            style = OffhandTheme.type.caption,
            color = OffhandTheme.colors.ink3,
            modifier = Modifier.padding(top = 14.dp),
        )
        ShareActions(
            onShare = { onShare(noteFormat.takeIf { isText }, !isText) },
            onSaveToDevice = { onSaveToDevice(noteFormat.takeIf { isText }, !isText) },
        )
    }
}

@Composable
private fun ShareTabs(selected: ShareTabUi, onSelected: (ShareTabUi) -> Unit) {
    val haptics = haptics()
    SegmentedControl(
        options = ShareTabUi.entries.map { stringResource(it.labelRes()) },
        selectedIndex = selected.ordinal,
        onSelect = { index ->
            haptics.tick()
            onSelected(ShareTabUi.entries[index])
        },
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun ShareFormatOptions(
    selected: NoteExportFormat,
    isDocumentExportUnlocked: Boolean,
    onSelected: (NoteExportFormat) -> Unit,
) {
    Column {
        NoteExportFormat.entries.forEach { format ->
            ChoiceRow(
                title = stringResource(format.labelRes()),
                hint = stringResource(format.hintRes()),
                isSelected = format == selected,
                showProBadge = format != NoteExportFormat.TEXT && !isDocumentExportUnlocked,
                showDivider = format != NoteExportFormat.entries.last(),
                onClick = { onSelected(format) },
            )
        }
    }
}

@Composable
private fun ShareAudioDetails() {
    val colors = OffhandTheme.colors
    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.notes_share_audio_format),
                style = OffhandTheme.type.body,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Text(text = stringResource(R.string.notes_share_audio_hint), style = OffhandTheme.type.number, color = colors.ink3)
        }
        Text(text = stringResource(R.string.notes_share_audio_details), style = OffhandTheme.type.bodySmall, color = colors.ink2)
    }
}

@Composable
private fun ShareActions(onShare: () -> Unit, onSaveToDevice: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PrimaryButton(
            text = stringResource(R.string.notes_share_dialog_confirm),
            onClick = onShare,
            modifier = Modifier.weight(1f),
        )
        SecondaryButton(
            text = stringResource(R.string.notes_share_download),
            onClick = onSaveToDevice,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun ShareTabUi.labelRes(): Int = when (this) {
    ShareTabUi.TEXT -> R.string.notes_share_tab_text
    ShareTabUi.AUDIO -> R.string.notes_share_tab_audio
}

private fun NoteExportFormat.labelRes(): Int = when (this) {
    NoteExportFormat.TEXT -> R.string.notes_share_format_text
    NoteExportFormat.PDF -> R.string.notes_share_format_pdf
    NoteExportFormat.DOCX -> R.string.notes_share_format_docx
}

private fun NoteExportFormat.hintRes(): Int = when (this) {
    NoteExportFormat.TEXT -> R.string.notes_share_format_text_hint
    NoteExportFormat.PDF -> R.string.notes_share_format_pdf_hint
    NoteExportFormat.DOCX -> R.string.notes_share_format_docx_hint
}
