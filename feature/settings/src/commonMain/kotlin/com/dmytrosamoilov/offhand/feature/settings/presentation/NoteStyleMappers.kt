package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle

internal fun CustomNoteStyle.toOptionUi(): CustomStyleOptionUi = CustomStyleOptionUi(
    id = id,
    name = name,
    description = sections.joinToString(separator = ", ") { it.heading },
)
