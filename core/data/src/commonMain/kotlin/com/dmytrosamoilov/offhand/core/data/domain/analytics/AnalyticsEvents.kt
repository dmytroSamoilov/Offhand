package com.dmytrosamoilov.offhand.core.data.domain.analytics

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan

enum class NoteSource { RECORDING, IMPORT, RETRY, RESTYLE }

enum class NoteSection { OVERVIEW, TRANSCRIPT }

// Every event the apps send. Nothing here carries user content: styles go
// out as their codename or "custom", never by name.
object AnalyticsEvents {

    fun noteRecorded(durationMs: Long?, style: NoteStyleRef): AnalyticsEvent =
        event("note_recorded", "duration_s" to seconds(durationMs), "style" to style.analyticsName())

    fun noteReady(source: NoteSource, durationMs: Long?, processingMs: Long): AnalyticsEvent = event(
        "note_ready",
        "source" to source.label(),
        "duration_s" to seconds(durationMs),
        "processing_s" to seconds(processingMs),
    )

    fun noteFailed(source: NoteSource): AnalyticsEvent = event("note_failed", "source" to source.label())

    fun audioImportStarted(files: Int): AnalyticsEvent = event("audio_import_started", "files" to files.toLong())

    fun audioImportRejected(reason: String): AnalyticsEvent = event("audio_import_rejected", "reason" to reason)

    fun noteCopied(section: NoteSection): AnalyticsEvent = event("note_copied", "section" to section.label())

    fun notePlayed(): AnalyticsEvent = event("note_played")

    fun notePaused(): AnalyticsEvent = event("note_paused")

    fun shareClicked(): AnalyticsEvent = event("share_clicked")

    fun shareCompleted(format: String, savedToDevice: Boolean): AnalyticsEvent =
        event("share_completed", "format" to format, "action" to if (savedToDevice) "save" else "share")

    fun noteEdited(): AnalyticsEvent = event("note_edited")

    fun noteStyleChanged(style: NoteStyleRef): AnalyticsEvent =
        event("note_style_changed", "style" to style.analyticsName())

    fun noteDeleted(): AnalyticsEvent = event("note_deleted")

    fun noteMoved(): AnalyticsEvent = event("note_moved")

    fun suggestionsRequested(): AnalyticsEvent = event("suggestions_requested")

    fun suggestionAdded(): AnalyticsEvent = event("suggestion_added")

    fun suggestionDismissed(): AnalyticsEvent = event("suggestion_dismissed")

    fun folderCreated(): AnalyticsEvent = event("folder_created")

    fun folderRenamed(): AnalyticsEvent = event("folder_renamed")

    fun folderDeleted(): AnalyticsEvent = event("folder_deleted")

    fun defaultStyleChanged(style: NoteStyleRef): AnalyticsEvent =
        event("default_style_changed", "style" to style.analyticsName())

    fun smartSuggestionsToggled(enabled: Boolean): AnalyticsEvent =
        event("smart_suggestions_toggled", "enabled" to enabled.label())

    fun appLockToggled(enabled: Boolean): AnalyticsEvent = event("app_lock_toggled", "enabled" to enabled.label())

    fun noteStyleCreated(sections: Int, described: Boolean): AnalyticsEvent = event(
        "note_style_created",
        "sections" to sections.toLong(),
        "source" to if (described) "described" else "manual",
    )

    fun noteStyleDeleted(): AnalyticsEvent = event("note_style_deleted")

    fun backupCreated(notes: Int, withAudio: Boolean): AnalyticsEvent =
        event("backup_created", "notes" to notes.toLong(), "with_audio" to withAudio.label())

    fun backupRestored(notes: Int): AnalyticsEvent = event("backup_restored", "notes" to notes.toLong())

    fun paywallShown(feature: ProFeature): AnalyticsEvent = event("paywall_shown", "feature" to feature.label())

    fun paywallDismissed(feature: ProFeature): AnalyticsEvent =
        event("paywall_dismissed", "feature" to feature.label())

    fun purchaseStarted(plan: ProPlan): AnalyticsEvent = event("purchase_started", "plan" to plan.label())

    fun purchaseCompleted(plan: ProPlan, trial: Boolean): AnalyticsEvent =
        event("purchase_completed", "plan" to plan.label(), "trial" to trial.label())

    fun restoreClicked(): AnalyticsEvent = event("restore_clicked")

    fun redeemCodeClicked(): AnalyticsEvent = event("redeem_code_clicked")

    fun onboardingCompleted(): AnalyticsEvent = event("onboarding_completed")

    fun modelDownloaded(durationMs: Long): AnalyticsEvent = event("model_downloaded", "duration_s" to seconds(durationMs))

    fun notesSearched(): AnalyticsEvent = event("notes_searched")

    private fun event(name: String, vararg params: Pair<String, Any>): AnalyticsEvent = AnalyticsEvent(name, params.toMap())

    private fun seconds(millis: Long?): Long = (millis ?: 0L) / MS_PER_SECOND

    private fun Enum<*>.label(): String = name.lowercase()

    private fun Boolean.label(): String = toString()

    private const val MS_PER_SECOND = 1_000L
}

fun NoteStyleRef.analyticsName(): String = when (this) {
    is NoteStyleRef.BuiltIn -> preset.name.lowercase()
    is NoteStyleRef.Custom -> CUSTOM_STYLE_NAME
}

private const val CUSTOM_STYLE_NAME = "custom"
