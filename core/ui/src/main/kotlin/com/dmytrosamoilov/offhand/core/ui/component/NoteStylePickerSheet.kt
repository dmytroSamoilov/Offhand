package com.dmytrosamoilov.offhand.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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

// One picker for the default style (Settings) and the restyle sheet (note):
// the user's own styles come first, the built-in ones after them.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteStylePickerSheet(
    title: String,
    body: String?,
    selected: NoteStyleRef,
    customStyles: List<NoteStyleChoice>,
    isCustomStylesUnlocked: Boolean,
    onSelected: (NoteStyleRef) -> Unit,
    onDismiss: () -> Unit,
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
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            body?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (customStyles.isNotEmpty()) {
                PickerHeader(text = stringResource(R.string.core_ui_styles_custom_header))
                customStyles.forEach { style ->
                    NoteStyleCard(
                        title = style.name,
                        description = style.description,
                        icon = CustomNoteStyleIcon,
                        isSelected = selected == NoteStyleRef.Custom(style.id),
                        onClick = { onSelected(NoteStyleRef.Custom(style.id)) },
                        showProBadge = !isCustomStylesUnlocked,
                    )
                }
                PickerHeader(text = stringResource(R.string.core_ui_styles_built_in_header))
            }
            NotePresetOption.entries.forEach { option ->
                val style = NoteStyleRef.BuiltIn(option.toDomain())
                NotePresetOptionCard(option = option, isSelected = selected == style, onClick = { onSelected(style) })
            }
        }
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
