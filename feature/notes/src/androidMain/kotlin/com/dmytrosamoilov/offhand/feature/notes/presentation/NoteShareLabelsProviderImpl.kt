package com.dmytrosamoilov.offhand.feature.notes.presentation

import android.content.Context
import com.dmytrosamoilov.offhand.feature.notes.R
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabels
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabelsProvider

class NoteShareLabelsProviderImpl(
    private val context: Context,
) : NoteShareLabelsProvider {

    override fun labels(): NoteShareLabels = NoteShareLabels(
        title = context.getString(R.string.notes_edit_title_label),
        date = context.getString(R.string.notes_share_date_label),
        overview = context.getString(R.string.notes_overview_heading),
        transcript = context.getString(R.string.notes_transcript_heading),
    )

    override fun fallbackTitle(): String = context.getString(R.string.notes_recording_fallback_title)
}
