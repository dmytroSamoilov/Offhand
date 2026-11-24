package com.dmytrosamoilov.offhand.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.requiredProFeature
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandSheet
import com.dmytrosamoilov.offhand.core.designsystem.component.PlainTextButton
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.core.ui.R

data class NoteStyleChoice(
    val id: Long,
    val name: String,
    val description: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteStylePickerSheet(
    title: String,
    body: String?,
    selected: NoteStyleRef?,
    customStyles: List<NoteStyleChoice>,
    isProStylesUnlocked: Boolean,
    onSelected: (NoteStyleRef) -> Unit,
    onDismiss: () -> Unit,
    onCreateStyle: (() -> Unit)? = null,
    onDefaultSelected: (() -> Unit)? = null,
) {
    OffhandSheet(
        onDismissRequest = onDismiss,
        title = title,
        titleTrailing = { onCreateStyle?.let { NewStyleButton(onClick = it) } },
    ) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            body?.let {
                Text(
                    text = it,
                    style = OffhandTheme.type.dialogBody,
                    color = OffhandTheme.colors.ink2,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            NoteStyleList(
                selected = selected,
                customStyles = customStyles,
                isProStylesUnlocked = isProStylesUnlocked,
                onSelected = onSelected,
                onDefaultSelected = onDefaultSelected,
            )
        }
    }
}

@Composable
fun ColumnScope.NoteStyleList(
    selected: NoteStyleRef?,
    customStyles: List<NoteStyleChoice>,
    isProStylesUnlocked: Boolean,
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
        customStyles.forEachIndexed { index, style ->
            NoteStyleCard(
                title = style.name,
                description = style.description,
                isSelected = selected == NoteStyleRef.Custom(style.id),
                onClick = { onSelected(NoteStyleRef.Custom(style.id)) },
                showProBadge = !isProStylesUnlocked,
                showDivider = index < customStyles.lastIndex,
                trailingActions = { customStyleActions(style) },
            )
        }
    }
    PickerHeader(text = stringResource(R.string.core_ui_styles_built_in_header))
    NotePresetOption.entries.forEach { option ->
        val style = NoteStyleRef.BuiltIn(option.toDomain())
        NotePresetOptionCard(
            option = option,
            isSelected = selected == style,
            onClick = { onSelected(style) },
            showProBadge = !isProStylesUnlocked && style.requiredProFeature() != null,
            showDivider = option != NotePresetOption.entries.last(),
        )
    }
}

@Composable
fun NewStyleButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PlainTextButton(text = stringResource(R.string.core_ui_styles_new_button), onClick = onClick, modifier = modifier)
}

@Composable
private fun PickerHeader(text: String) {
    ReadableSectionLabel(text = text, modifier = Modifier.padding(top = 22.dp, bottom = 2.dp))
}

@Composable
fun NoteStyleRef.label(customStyles: List<NoteStyleChoice>): String = when (this) {
    is NoteStyleRef.BuiltIn -> stringResource(preset.toUi().labelRes)
    is NoteStyleRef.Custom -> customStyles.firstOrNull { it.id == id }?.name
        ?: stringResource(NotePresetOption.SUMMARY.labelRes)
}
