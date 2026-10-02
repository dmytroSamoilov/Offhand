package com.dmytrosamoilov.offhand.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.ui.R

data class NoteStyleChoice(
    val id: Long,
    val name: String,
    val description: String,
)

// The restyle sheet: the same list the Summary styles screen shows, inside a
// bottom sheet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteStylePickerSheet(
    title: String,
    body: String?,
    selected: NoteStyleRef?,
    customStyles: List<NoteStyleChoice>,
    isCustomStylesUnlocked: Boolean,
    onSelected: (NoteStyleRef) -> Unit,
    onDismiss: () -> Unit,
    onCreateStyle: (() -> Unit)? = null,
    onDefaultSelected: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                onCreateStyle?.let { NewStyleButton(onClick = it) }
            }
            body?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            NoteStyleList(
                selected = selected,
                customStyles = customStyles,
                isCustomStylesUnlocked = isCustomStylesUnlocked,
                onSelected = onSelected,
                onDefaultSelected = onDefaultSelected,
            )
        }
    }
}

// One list for every place a style is picked: the user's own styles, then
// the built-in ones; "New" sits in the top-right corner of whatever hosts it.
// The styles screen adds an Edit action to the custom rows; a folder's picker
// adds a "Default" row on top, selected when the folder has no style of its
// own (selected == null).
@Composable
fun ColumnScope.NoteStyleList(
    selected: NoteStyleRef?,
    customStyles: List<NoteStyleChoice>,
    isCustomStylesUnlocked: Boolean,
    onSelected: (NoteStyleRef) -> Unit,
    onDefaultSelected: (() -> Unit)? = null,
    customStyleActions: @Composable RowScope.(NoteStyleChoice) -> Unit = {},
) {
    onDefaultSelected?.let {
        NoteStyleCard(
            title = stringResource(R.string.core_ui_styles_default_option),
            description = stringResource(R.string.core_ui_styles_default_option_description),
            isSelected = selected == null,
            onClick = it,
        )
    }
    if (customStyles.isNotEmpty()) {
        PickerHeader(text = stringResource(R.string.core_ui_styles_custom_header))
        customStyles.forEach { style ->
            NoteStyleCard(
                title = style.name,
                description = style.description,
                isSelected = selected == NoteStyleRef.Custom(style.id),
                onClick = { onSelected(NoteStyleRef.Custom(style.id)) },
                showProBadge = !isCustomStylesUnlocked,
                trailingActions = { customStyleActions(style) },
            )
        }
    }
    PickerHeader(text = stringResource(R.string.core_ui_styles_built_in_header))
    NotePresetOption.entries.forEach { option ->
        val style = NoteStyleRef.BuiltIn(option.toDomain())
        NotePresetOptionCard(option = option, isSelected = selected == style, onClick = { onSelected(style) })
    }
}

@Composable
fun NewStyleButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(text = stringResource(R.string.core_ui_styles_new_button))
    }
}

@Composable
private fun PickerHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
fun NoteStyleRef.label(customStyles: List<NoteStyleChoice>): String = when (this) {
    is NoteStyleRef.BuiltIn -> stringResource(preset.toUi().labelRes)
    is NoteStyleRef.Custom -> customStyles.firstOrNull { it.id == id }?.name
        ?: stringResource(NotePresetOption.SUMMARY.labelRes)
}
