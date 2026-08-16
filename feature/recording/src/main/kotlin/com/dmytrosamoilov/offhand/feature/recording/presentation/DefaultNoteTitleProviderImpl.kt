package com.dmytrosamoilov.offhand.feature.recording.presentation

import android.content.Context
import com.dmytrosamoilov.offhand.feature.recording.R
import com.dmytrosamoilov.offhand.feature.recording.domain.DefaultNoteTitleProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultNoteTitleProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : DefaultNoteTitleProvider {

    override fun titleFor(nextNumber: Int): String =
        context.getString(R.string.recording_default_note_title, nextNumber)
}
