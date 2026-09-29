package com.dmytrosamoilov.offhand.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.designsystem.component.ProBadge
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
    onCreateStyle: (() -> Unit)? = null,
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
            onCreateStyle?.let { CreateStyleButton(isCustomStylesUnlocked = isCustomStylesUnlocked, onClick = it) }
        }
    }
}

@Composable
private fun CreateStyleButton(isCustomStylesUnlocked: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.core_ui_styles_create_button))
            if (!isCustomStylesUnlocked) {
                Spacer(modifier = Modifier.width(6.dp))
                ProBadge()
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
