package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.designsystem.component.Chip
import com.dmytrosamoilov.offhand.core.designsystem.component.SectionLabel
import com.dmytrosamoilov.offhand.core.ui.component.toUi
import com.dmytrosamoilov.offhand.feature.notes.R

@Composable
internal fun StyleChipsRow(
    chips: List<StyleChipUi>,
    onStyleSelected: (NoteStyleRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(
            text = stringResource(R.string.notes_folder_style),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(chips, key = { it.style.toString() }) { chip ->
                Chip(
                    text = chip.label(),
                    onClick = { if (!chip.isCurrent) onStyleSelected(chip.style) },
                    isSelected = chip.isCurrent,
                )
            }
        }
    }
}

@Composable
private fun StyleChipUi.label(): String = customName ?: when (val ref = style) {
    is NoteStyleRef.BuiltIn -> stringResource(ref.preset.toUi().labelRes)
    is NoteStyleRef.Custom -> ""
}
