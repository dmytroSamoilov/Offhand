package com.dmytrosamoilov.offhand.feature.recording.presentation

import android.content.Context
import com.dmytrosamoilov.offhand.feature.recording.R
import com.dmytrosamoilov.offhand.feature.recording.domain.DefaultNoteTitleProvider

class DefaultNoteTitleProviderImpl(
    private val context: Context,
) : DefaultNoteTitleProvider {

    override fun titleFor(nextNumber: Int): String =
        context.getString(R.string.recording_default_note_title, nextNumber)

    override fun untitledTitle(): String =
        context.getString(R.string.recording_untitled_note_title)
}
