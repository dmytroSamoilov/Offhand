package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandDialog
import com.dmytrosamoilov.offhand.core.designsystem.component.PlainTextButton
import com.dmytrosamoilov.offhand.core.designsystem.component.SegmentedControl
import com.dmytrosamoilov.offhand.core.designsystem.component.TextButtonTone
import com.dmytrosamoilov.offhand.core.designsystem.component.TopBar
import com.dmytrosamoilov.offhand.core.designsystem.haptics.haptics
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.core.ui.component.LabeledTextField
import com.dmytrosamoilov.offhand.feature.settings.R
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleFieldError
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleValidator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NoteStyleEditorScreen(
    styleId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteStyleEditorViewModel = koinViewModel(parameters = { parametersOf(styleId) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved, state.isDeleted) {
        if (state.isSaved || state.isDeleted) onBack()
    }
    if (state.isDeleteRequested) {
        DeleteStyleDialog(onConfirm = viewModel::onDeleteConfirmed, onDismiss = viewModel::onDeleteDismissed)
    }
    BaseComposeScreen(viewModel = viewModel, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            EditorTopBar(isNew = state.isNew, onCancel = onBack, onSave = viewModel::onSaveRequested)
            SettingsPage(topBar = {}) {
                EditorContent(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun EditorTopBar(isNew: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    TopBar(
        title = stringResource(
            if (isNew) R.string.settings_note_style_editor_new_title else R.string.settings_note_style_editor_edit_title,
        ),
        leading = {
            PlainTextButton(
                text = stringResource(R.string.settings_delete_model_dialog_cancel),
                onClick = onCancel,
                tone = TextButtonTone.Ink,
            )
        },
        trailing = {
            PlainTextButton(text = stringResource(R.string.settings_note_style_editor_save), onClick = onSave)
        },
    )
}

@Composable
private fun EditorContent(state: NoteStyleEditorUiState, viewModel: NoteStyleEditorViewModel) {
    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        DescribeStyleCard(
            state = state.describe ?: DescribeStyleUi(),
            onDescriptionChanged = viewModel::onDescriptionChanged,
            onBuild = viewModel::onDraftRequested,
        )
        LabeledTextField(
            label = stringResource(R.string.settings_note_style_editor_name),
            value = state.name,
            onValueChange = viewModel::onNameChanged,
            error = fieldErrorText(state.errors.name, NoteStyleValidator.MAX_NAME_LENGTH),
        )
        LabeledTextField(
            label = stringResource(R.string.settings_note_style_editor_kind),
            value = state.noteKind,
            onValueChange = viewModel::onNoteKindChanged,
            placeholder = stringResource(R.string.settings_note_style_editor_kind_hint),
            error = fieldErrorText(state.errors.noteKind, NoteStyleValidator.MAX_KIND_LENGTH),
            help = stringResource(R.string.settings_note_style_editor_kind_help),
        )
        LanguageChoice(selected = state.language, onSelected = viewModel::onLanguageChanged)
    }
    SectionsEditor(state = state, viewModel = viewModel)
    if (!state.isNew) {
        PlainTextButton(
            text = stringResource(R.string.settings_note_styles_delete),
            onClick = viewModel::onDeleteRequested,
            tone = TextButtonTone.Danger,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Composable
private fun DeleteStyleDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    OffhandDialog(
        title = stringResource(R.string.settings_note_styles_delete_dialog_title),
        body = stringResource(R.string.settings_note_styles_delete_dialog_body),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.settings_delete_model_dialog_cancel),
        onConfirm = onDismiss,
        destructiveLabel = stringResource(R.string.settings_note_styles_delete_dialog_confirm),
        onDestructive = onConfirm,
    )
}

@Composable
internal fun fieldErrorText(error: NoteStyleFieldError?, maxLength: Int): String? = when (error) {
    null -> null
    NoteStyleFieldError.BLANK -> stringResource(R.string.settings_note_style_error_blank)
    NoteStyleFieldError.TOO_LONG -> stringResource(R.string.settings_note_style_error_too_long, maxLength)
    NoteStyleFieldError.DUPLICATE -> stringResource(R.string.settings_note_style_error_duplicate)
}

@Composable
private fun LanguageChoice(selected: NoteStyleLanguage, onSelected: (NoteStyleLanguage) -> Unit) {
    val haptics = haptics()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.settings_note_style_editor_language),
            style = OffhandTheme.type.caption.copy(fontWeight = OffhandTheme.type.button.fontWeight),
            color = OffhandTheme.colors.ink,
        )
        SegmentedControl(
            options = NoteStyleLanguage.entries.map { stringResource(it.labelRes()) },
            selectedIndex = NoteStyleLanguage.entries.indexOf(selected),
            onSelect = { index ->
                haptics.tick()
                onSelected(NoteStyleLanguage.entries[index])
            },
            compact = true,
        )
    }
}

private fun NoteStyleLanguage.labelRes(): Int = when (this) {
    NoteStyleLanguage.RECORDING -> R.string.settings_note_style_editor_language_recording
    NoteStyleLanguage.ENGLISH -> R.string.settings_note_style_editor_language_english
}
