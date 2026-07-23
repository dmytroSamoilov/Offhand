package com.dmytrosamoilov.offhand.feature.notes.domain

import android.net.Uri

data class NoteShareBundle(
    val uris: List<Uri>,
    val mimeType: String,
)
