package com.dmytrosamoilov.offhand.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import com.dmytrosamoilov.offhand.core.designsystem.component.SectionLabel

@Composable
fun ReadableSectionLabel(text: String, modifier: Modifier = Modifier) {
    SectionLabel(
        text = text,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = text
            heading()
        },
    )
}
