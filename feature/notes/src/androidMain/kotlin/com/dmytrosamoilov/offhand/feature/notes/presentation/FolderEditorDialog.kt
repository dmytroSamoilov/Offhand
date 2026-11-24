package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandDialog
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandRadius
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.notes.R
import com.dmytrosamoilov.offhand.feature.notes.domain.FolderNameValidator

@Composable
internal fun FolderEditorDialog(
    editor: FolderEditorUi,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    OffhandDialog(
        title = stringResource(if (editor.folderId == null) R.string.notes_folder_new else R.string.notes_folder_rename),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.notes_folder_save),
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.notes_delete_dialog_cancel),
    ) {
        FolderNameField(editor = editor, onNameChanged = onNameChanged, onDone = onConfirm)
    }
}

@Composable
private fun FolderNameField(
    editor: FolderEditorUi,
    onNameChanged: (String) -> Unit,
    onDone: () -> Unit,
) {
    var value by remember(editor.folderId) {
        mutableStateOf(TextFieldValue(editor.name, TextRange(0, editor.name.length)))
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val error = editor.error?.message()
    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.notes_folder_name_label),
            style = OffhandTheme.type.caption,
            fontWeight = FontWeight.SemiBold,
            color = OffhandTheme.colors.ink,
        )
        NameBox(
            value = value,
            onValueChange = { changed ->
                value = changed
                onNameChanged(changed.text)
            },
            isError = error != null,
            onDone = onDone,
            modifier = Modifier.focusRequester(focusRequester),
        )
        error?.let { Text(text = it, style = OffhandTheme.type.caption, color = OffhandTheme.colors.danger) }
    }
}

@Composable
private fun NameBox(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isError: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OffhandTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(OffhandRadius.field)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = OffhandTheme.type.body.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.ink),
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .background(colors.surface, shape)
                    .border(
                        width = if (isFocused || isError) 1.5.dp else 1.dp,
                        color = if (isError) colors.danger else if (isFocused) colors.ink else colors.border,
                        shape = shape,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) { inner() }
        },
    )
}

@Composable
private fun FolderNameErrorUi.message(): String = when (this) {
    FolderNameErrorUi.BLANK -> stringResource(R.string.notes_folder_error_blank)
    FolderNameErrorUi.TOO_LONG -> stringResource(R.string.notes_folder_error_too_long, FolderNameValidator.MAX_LENGTH)
    FolderNameErrorUi.DUPLICATE -> stringResource(R.string.notes_folder_error_duplicate)
}
