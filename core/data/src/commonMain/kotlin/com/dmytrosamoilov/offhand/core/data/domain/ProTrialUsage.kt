package com.dmytrosamoilov.offhand.core.data.domain

// What someone did with Pro during the free trial, counted on the phone and
// never sent anywhere, so the paywall that follows the trial can show it
// back. The order is the order of importance when only a few lines fit.
enum class ProTrialUsage {
    MEETING_NOTES,
    VISIT_NOTES,
    LEGAL_NOTES,
    EMAIL_NOTES,
    CUSTOM_STYLE_NOTES,
    PDF_EXPORTS,
    WORD_EXPORTS,
    IMPORTS,
    CALENDAR_EVENTS,
    FOLDER_STYLES,
    AUDIO_BACKUPS,
}

data class ProTrialUsageCount(
    val usage: ProTrialUsage,
    val count: Int,
)

interface ProTrialUsageRepository {

    suspend fun add(usage: ProTrialUsage, count: Int)

    suspend fun read(): List<ProTrialUsageCount>
}
