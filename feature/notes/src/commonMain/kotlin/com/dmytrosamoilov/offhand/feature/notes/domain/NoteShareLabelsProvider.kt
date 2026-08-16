package com.dmytrosamoilov.offhand.feature.notes.domain

interface NoteShareLabelsProvider {

    fun labels(): NoteShareLabels

    fun fallbackTitle(): String
}
