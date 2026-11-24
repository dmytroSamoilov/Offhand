package com.dmytrosamoilov.offhand.core.data.domain.analytics

import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsage
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsageCount

// The analytics events already mark every use of a Pro feature, so the
// trial's usage counters read them instead of adding a second hook to each
// feature. The names and values here are the ones AnalyticsEvents writes.
fun AnalyticsEvent.proTrialUsage(): ProTrialUsageCount? = when (name) {
    "note_recorded", "note_style_changed" -> styleUsage(params["style"])?.once()
    "share_completed" -> exportUsage(params["format"])?.once()
    "audio_import_started" -> (params["files"] as? Long)?.takeIf { it > 0 }
        ?.let { ProTrialUsageCount(ProTrialUsage.IMPORTS, it.toInt()) }
    "suggestion_added" -> ProTrialUsage.CALENDAR_EVENTS.once()
    "folder_style_changed" -> ProTrialUsage.FOLDER_STYLES.takeIf { params["style"] != "default" }?.once()
    "backup_created" -> ProTrialUsage.AUDIO_BACKUPS.takeIf { params["with_audio"] == "true" }?.once()
    else -> null
}

private fun styleUsage(style: Any?): ProTrialUsage? = when (style) {
    "meeting" -> ProTrialUsage.MEETING_NOTES
    "visit" -> ProTrialUsage.VISIT_NOTES
    "legal" -> ProTrialUsage.LEGAL_NOTES
    "email" -> ProTrialUsage.EMAIL_NOTES
    "custom" -> ProTrialUsage.CUSTOM_STYLE_NOTES
    else -> null
}

private fun exportUsage(format: Any?): ProTrialUsage? = when (format) {
    "pdf" -> ProTrialUsage.PDF_EXPORTS
    "docx" -> ProTrialUsage.WORD_EXPORTS
    else -> null
}

private fun ProTrialUsage.once() = ProTrialUsageCount(this, 1)
