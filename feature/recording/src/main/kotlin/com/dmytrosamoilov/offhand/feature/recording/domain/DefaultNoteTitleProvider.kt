package com.dmytrosamoilov.offhand.feature.recording.domain

interface DefaultNoteTitleProvider {

    fun titleFor(nextNumber: Int): String
}
