package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandTextField
import com.dmytrosamoilov.offhand.core.designsystem.component.PlainTextButton
import com.dmytrosamoilov.offhand.core.designsystem.component.TextButtonTone
import com.dmytrosamoilov.offhand.core.designsystem.component.TopBar
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.notes.R

@Composable
internal fun NoteEditor(
    editor: NoteEditorUi,
    onTitleChanged: (String) -> Unit,
    onTranscriptChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(OffhandTheme.colors.ground)) {
        NoteEditorTopBar(onSave = onSave, onCancel = onCancel)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OffhandTextField(
                value = editor.title,
                onValueChange = onTitleChanged,
                label = stringResource(R.string.notes_edit_title_label),
            )
            OffhandTextField(
                value = editor.transcript,
                onValueChange = onTranscriptChanged,
                label = stringResource(R.string.notes_transcript_heading),
                singleLine = false,
                minLines = TRANSCRIPT_MIN_LINES,
            )
        }
    }
}

@Composable
private fun NoteEditorTopBar(onSave: () -> Unit, onCancel: () -> Unit) {
    val cancelDescription = stringResource(R.string.notes_edit_cancel_description)
    val saveDescription = stringResource(R.string.notes_edit_save_description)
    TopBar(
        title = stringResource(R.string.notes_edit_description),
        leading = {
            PlainTextButton(
                text = stringResource(R.string.notes_delete_dialog_cancel),
                onClick = onCancel,
                tone = TextButtonTone.Ink,
                modifier = Modifier.semantics { contentDescription = cancelDescription },
            )
        },
        trailing = {
            PlainTextButton(
                text = stringResource(R.string.notes_folder_save),
                onClick = onSave,
                modifier = Modifier.semantics { contentDescription = saveDescription },
            )
        },
    )
}

private const val TRANSCRIPT_MIN_LINES = 10
