package com.dmytrosamoilov.offhand.feature.paywall.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsage
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsageCount
import com.dmytrosamoilov.offhand.core.ui.R as CoreUiR
import com.dmytrosamoilov.offhand.feature.paywall.R

internal data class ComparisonRow(
    val titleRes: Int,
    val hintRes: Int?,
    val features: Set<ProFeature>,
    val needsSummary: Boolean = false,
    val isHintAboutStyles: Boolean = false,
) {
    val isPro: Boolean get() = features.isNotEmpty()

    fun forPhone(isSummaryAvailable: Boolean): ComparisonRow? = when {
        isSummaryAvailable -> this
        needsSummary -> null
        isHintAboutStyles -> copy(hintRes = null)
        else -> this
    }
}

private val comparisonRows = listOf(
    ComparisonRow(R.string.paywall_free_summary, null, emptySet(), needsSummary = true),
    ComparisonRow(R.string.paywall_free_private, null, emptySet()),
    ComparisonRow(R.string.paywall_free_offline, null, emptySet()),
    ComparisonRow(R.string.paywall_free_no_ads, null, emptySet()),
    ComparisonRow(R.string.paywall_free_unlimited, null, emptySet()),
    ComparisonRow(R.string.paywall_free_backup, null, emptySet()),
    ComparisonRow(
        R.string.paywall_pro_styles,
        R.string.paywall_pro_styles_hint,
        setOf(ProFeature.CUSTOM_STYLES, ProFeature.NOTE_STYLES),
        needsSummary = true,
    ),
    ComparisonRow(R.string.paywall_pro_export, R.string.paywall_pro_export_hint, setOf(ProFeature.DOCUMENT_EXPORT)),
    ComparisonRow(
        R.string.paywall_pro_suggestions,
        R.string.paywall_pro_suggestions_hint,
        setOf(ProFeature.SMART_SUGGESTIONS),
        needsSummary = true,
    ),
    ComparisonRow(R.string.paywall_pro_import, R.string.paywall_pro_import_hint, setOf(ProFeature.AUDIO_IMPORT)),
    ComparisonRow(
        R.string.paywall_pro_folders,
        R.string.paywall_pro_folders_hint,
        setOf(ProFeature.FOLDER_STYLES, ProFeature.FOLDERS),
        isHintAboutStyles = true,
    ),
    ComparisonRow(R.string.paywall_pro_backup, R.string.paywall_pro_backup_hint, setOf(ProFeature.BACKUP_AUDIO)),
)

internal fun sortedRows(highlighted: ProFeature, isSummaryAvailable: Boolean): List<ComparisonRow> =
    comparisonRows
        .mapNotNull { it.forPhone(isSummaryAvailable) }
        .sortedWith(compareBy({ highlighted !in it.features }, { !it.isPro }))

internal val PaywallUiState.shownTrialRecap: List<ProTrialUsageCount>
    get() = if (isSummaryAvailable) trialRecap else trialRecap.filterNot { it.usage.needsSummary }

private val ProTrialUsage.needsSummary: Boolean
    get() = when (this) {
        ProTrialUsage.PDF_EXPORTS, ProTrialUsage.WORD_EXPORTS, ProTrialUsage.IMPORTS, ProTrialUsage.AUDIO_BACKUPS -> false
        ProTrialUsage.MEETING_NOTES, ProTrialUsage.VISIT_NOTES, ProTrialUsage.LEGAL_NOTES, ProTrialUsage.EMAIL_NOTES,
        ProTrialUsage.CUSTOM_STYLE_NOTES, ProTrialUsage.CALENDAR_EVENTS, ProTrialUsage.FOLDER_STYLES,
        -> true
    }

@Composable
internal fun ProOfferUi.priceLabel(): String = when (plan) {
    ProPlan.YEARLY -> stringResource(R.string.paywall_plan_yearly_price, price)
    ProPlan.MONTHLY -> stringResource(R.string.paywall_plan_monthly_price, price)
    else -> price
}

@Composable
internal fun ProOfferUi.hintLabel(): String = when {
    plan == ProPlan.LIFETIME -> stringResource(R.string.paywall_plan_lifetime_hint)
    trialDays > 0 -> pluralStringResource(R.plurals.paywall_plan_trial, trialDays, trialDays)
    plan == ProPlan.MONTHLY -> stringResource(R.string.paywall_plan_monthly_renews)
    else -> stringResource(R.string.paywall_plan_yearly_renews)
}

@Composable
internal fun ProOfferUi.secondHintLabel(): String? = when (plan) {
    ProPlan.YEARLY -> monthlyPrice?.let { stringResource(R.string.paywall_plan_yearly_monthly, it) }
    ProPlan.LIFETIME -> yearsOfYearly?.let { pluralStringResource(R.plurals.paywall_plan_lifetime_vs_yearly, it, it) }
    ProPlan.NONE, ProPlan.MONTHLY, ProPlan.INCLUDED, ProPlan.TRIAL -> null
}

@Composable
internal fun ProOfferUi.disclosure(): String? = when {
    plan == ProPlan.MONTHLY -> stringResource(R.string.paywall_monthly_disclosure, price)
    plan != ProPlan.YEARLY -> null
    trialDays > 0 -> stringResource(R.string.paywall_no_charge) + "\n" +
        pluralStringResource(R.plurals.paywall_trial_disclosure, trialDays, trialDays, price)
    else -> stringResource(R.string.paywall_yearly_disclosure, price)
}

@Composable
internal fun ProOfferUi?.ctaLabel(): String = when {
    this == null -> stringResource(R.string.paywall_cta_trial)
    plan == ProPlan.YEARLY && trialDays > 0 -> pluralStringResource(R.plurals.paywall_cta_trial_days, trialDays, trialDays)
    plan == ProPlan.YEARLY || plan == ProPlan.MONTHLY -> stringResource(R.string.paywall_cta_subscribe)
    else -> stringResource(R.string.paywall_cta_lifetime)
}

internal fun ProOfferUi?.buyNowLabelRes(): Int =
    if (this?.plan == ProPlan.LIFETIME) R.string.paywall_cta_lifetime else R.string.paywall_cta_subscribe

internal fun PaywallUiState.headlineRes(): Int = when {
    feature != ProFeature.TRIAL_ENDED -> feature.headlineRes()
    shownTrialRecap.isEmpty() -> R.string.paywall_trial_ended_title
    else -> R.string.paywall_trial_recap_title
}

internal fun PaywallUiState.taglineRes(): Int = when {
    feature == ProFeature.GENERAL -> R.string.paywall_tagline
    feature == ProFeature.FREE_TRIAL -> R.string.paywall_trial_intro_tagline
    feature != ProFeature.TRIAL_ENDED -> R.string.paywall_context_tagline
    shownTrialRecap.isEmpty() -> R.string.paywall_trial_ended_body
    else -> R.string.paywall_trial_recap_tagline
}

internal fun ProTrialUsage.labelRes(): Int = when (this) {
    ProTrialUsage.MEETING_NOTES -> CoreUiR.string.core_ui_note_preset_meeting
    ProTrialUsage.VISIT_NOTES -> CoreUiR.string.core_ui_note_preset_visit
    ProTrialUsage.LEGAL_NOTES -> CoreUiR.string.core_ui_note_preset_legal
    ProTrialUsage.EMAIL_NOTES -> CoreUiR.string.core_ui_note_preset_email
    ProTrialUsage.CUSTOM_STYLE_NOTES -> R.string.paywall_usage_custom_styles
    ProTrialUsage.PDF_EXPORTS -> R.string.paywall_usage_pdf
    ProTrialUsage.WORD_EXPORTS -> R.string.paywall_usage_word
    ProTrialUsage.IMPORTS -> R.string.paywall_usage_imports
    ProTrialUsage.CALENDAR_EVENTS -> R.string.paywall_usage_calendar
    ProTrialUsage.FOLDER_STYLES -> R.string.paywall_usage_folder_styles
    ProTrialUsage.AUDIO_BACKUPS -> R.string.paywall_usage_backups
}

internal fun ProPlan.titleRes(): Int = when (this) {
    ProPlan.LIFETIME -> R.string.paywall_plan_lifetime
    ProPlan.MONTHLY -> R.string.paywall_plan_monthly
    else -> R.string.paywall_plan_yearly
}

internal fun ProFeature.headlineRes(): Int = when (this) {
    ProFeature.GENERAL -> R.string.paywall_title
    ProFeature.CUSTOM_STYLES -> R.string.paywall_context_styles
    ProFeature.DOCUMENT_EXPORT -> R.string.paywall_context_export
    ProFeature.SMART_SUGGESTIONS -> R.string.paywall_context_suggestions
    ProFeature.AUDIO_IMPORT -> R.string.paywall_context_import
    ProFeature.FOLDER_STYLES -> R.string.paywall_context_folder_styles
    ProFeature.NOTE_STYLES -> R.string.paywall_context_note_styles
    ProFeature.FOLDERS -> R.string.paywall_context_folders
    ProFeature.BACKUP_AUDIO -> R.string.paywall_context_backup_audio
    ProFeature.WIDGET -> R.string.paywall_context_widget
    ProFeature.SKIP_SILENCE -> R.string.paywall_context_skip_silence
    ProFeature.FREE_TRIAL -> R.string.paywall_title
    ProFeature.TRIAL_ENDED -> R.string.paywall_trial_ended_title
}
