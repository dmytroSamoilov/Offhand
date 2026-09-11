package com.dmytrosamoilov.offhand.feature.paywall.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.common.LegalLinks
import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.designsystem.component.ProCrown
import com.dmytrosamoilov.offhand.core.designsystem.component.ProGold
import com.dmytrosamoilov.offhand.feature.paywall.R
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun PaywallScreen(viewModel: PaywallViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onOpened() }
    LaunchedEffect(state.isPro) {
        if (state.isPro) {
            delay(SUCCESS_DISMISS_MS)
            viewModel.onClosed()
        }
    }
    BackHandler(onBack = viewModel::onClosed)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        if (state.isPro) {
            SuccessContent(feature = state.feature)
        } else {
            PaywallContent(state = state, viewModel = viewModel)
        }
    }
    state.message?.let { message ->
        PaywallMessageDialog(message = message, onDismiss = viewModel::onMessageDismissed)
    }
}

@Composable
private fun PaywallContent(state: PaywallUiState, viewModel: PaywallViewModel) {
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        CloseRow(onClose = viewModel::onClosed)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(feature = state.feature)
            ComparisonTable(highlighted = state.feature)
            PlanSection(state = state, onPlanSelected = viewModel::onPlanSelected, onRetry = viewModel::onRetryOffers)
            Spacer(modifier = Modifier.height(4.dp))
        }
        BottomActions(
            state = state,
            onPurchase = viewModel::onPurchaseClicked,
            onRestore = viewModel::onRestoreClicked,
            onContinueFree = viewModel::onClosed,
        )
    }
}

@Composable
private fun CloseRow(onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
        IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.paywall_close))
        }
    }
}

@Composable
private fun Header(feature: ProFeature) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ProCrown(size = 48.dp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(feature.headlineRes()),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(if (feature == ProFeature.GENERAL) R.string.paywall_tagline else R.string.paywall_context_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private data class ComparisonRow(val titleRes: Int, val hintRes: Int?, val feature: ProFeature?)

private val comparisonRows = listOf(
    ComparisonRow(R.string.paywall_free_private, null, null),
    ComparisonRow(R.string.paywall_free_offline, null, null),
    ComparisonRow(R.string.paywall_free_no_ads, null, null),
    ComparisonRow(R.string.paywall_free_unlimited, null, null),
    ComparisonRow(R.string.paywall_free_backup, null, null),
    ComparisonRow(R.string.paywall_pro_styles, R.string.paywall_pro_styles_hint, ProFeature.CUSTOM_STYLES),
    ComparisonRow(R.string.paywall_pro_export, R.string.paywall_pro_export_hint, ProFeature.DOCUMENT_EXPORT),
    ComparisonRow(R.string.paywall_pro_suggestions, R.string.paywall_pro_suggestions_hint, ProFeature.SMART_SUGGESTIONS),
    ComparisonRow(R.string.paywall_pro_import, R.string.paywall_pro_import_hint, ProFeature.AUDIO_IMPORT),
)

@Composable
private fun ComparisonTable(highlighted: ProFeature) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            TableHeader()
            comparisonRows.forEachIndexed { index, row ->
                if (index == FREE_ROW_COUNT) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                TableRow(row = row, isHighlighted = row.feature != null && row.feature == highlighted)
            }
            Text(
                text = stringResource(R.string.paywall_pro_more),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun TableHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.paywall_column_free),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(COLUMN_WIDTH),
        )
        Row(modifier = Modifier.width(COLUMN_WIDTH), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            ProCrown(size = 14.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = stringResource(R.string.paywall_column_pro), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TableRow(row: ComparisonRow, isHighlighted: Boolean) {
    val background = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(background, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(row.titleRes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
            )
            row.hintRes?.let { hint ->
                Text(text = stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        CheckCell(isIncluded = row.feature == null, tint = MaterialTheme.colorScheme.primary)
        CheckCell(isIncluded = true, tint = ProGold)
    }
}

@Composable
private fun CheckCell(isIncluded: Boolean, tint: Color) {
    Box(modifier = Modifier.width(COLUMN_WIDTH), contentAlignment = Alignment.Center) {
        if (isIncluded) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        } else {
            Text(text = stringResource(R.string.paywall_not_included), color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun PlanSection(state: PaywallUiState, onPlanSelected: (ProPlan) -> Unit, onRetry: () -> Unit) {
    when {
        state.isLoadingOffers -> Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.offers.isEmpty() -> OffersUnavailable(onRetry = onRetry)
        else -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            state.offers.forEach { offer ->
                PlanCard(
                    offer = offer,
                    isSelected = offer.plan == state.selectedPlan,
                    onClick = { onPlanSelected(offer.plan) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OffersUnavailable(onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.paywall_offers_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) { Text(text = stringResource(R.string.paywall_retry)) }
    }
}

@Composable
private fun PlanCard(offer: ProOfferUi, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val outline = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Card(
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PlanCardTitle(offer = offer, isSelected = isSelected)
            Text(text = offer.priceLabel(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = offer.hintLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            offer.secondHintLabel()?.let { hint ->
                Text(text = hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlanCardTitle(offer: ProOfferUi, isSelected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = stringResource(offer.plan.titleRes()), style = MaterialTheme.typography.titleSmall)
        if (offer.plan == ProPlan.LIFETIME) BestValueBadge()
        Spacer(modifier = Modifier.weight(1f))
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun BestValueBadge() {
    Surface(color = ProGold.copy(alpha = 0.18f), shape = RoundedCornerShape(6.dp)) {
        Text(
            text = stringResource(R.string.paywall_plan_best_value),
            style = MaterialTheme.typography.labelSmall,
            color = ProGold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun BottomActions(state: PaywallUiState, onPurchase: () -> Unit, onRestore: () -> Unit, onContinueFree: () -> Unit) {
    val context = LocalContext.current
    val offer = state.selectedOffer
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Button(onClick = onPurchase, enabled = offer != null && !state.isPurchasing, modifier = Modifier.fillMaxWidth()) {
            Text(text = offer.ctaLabel())
        }
        offer?.disclosure()?.let { disclosure ->
            Text(
                text = disclosure,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        TextButton(onClick = onContinueFree) { Text(text = stringResource(R.string.paywall_continue_free)) }
        Text(
            text = stringResource(R.string.paywall_privacy_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onRestore, enabled = !state.isPurchasing) { Text(text = stringResource(R.string.paywall_restore)) }
            TextButton(onClick = { openLink(context, LegalLinks.TERMS) }) { Text(text = stringResource(R.string.paywall_terms)) }
            TextButton(onClick = { openLink(context, LegalLinks.PRIVACY_POLICY) }) { Text(text = stringResource(R.string.paywall_privacy)) }
        }
    }
}

@Composable
private fun SuccessContent(feature: ProFeature) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ProCrown(size = 72.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.paywall_success_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(if (feature == ProFeature.GENERAL) R.string.paywall_success_body else R.string.paywall_success_continue),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PaywallMessageDialog(message: PaywallMessageUi, onDismiss: () -> Unit) {
    val text = when (message) {
        PaywallMessageUi.Failed -> R.string.paywall_message_failed
        PaywallMessageUi.Pending -> R.string.paywall_message_pending
        PaywallMessageUi.NothingToRestore -> R.string.paywall_message_restore_nothing
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.paywall_title)) },
        text = { Text(text = stringResource(text)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.paywall_message_dismiss)) } },
    )
}

@Composable
private fun ProOfferUi.priceLabel(): String = when (plan) {
    ProPlan.YEARLY -> stringResource(R.string.paywall_plan_yearly_price, price)
    else -> price
}

@Composable
private fun ProOfferUi.hintLabel(): String = when {
    plan == ProPlan.LIFETIME -> stringResource(R.string.paywall_plan_lifetime_hint)
    trialDays > 0 -> pluralStringResource(R.plurals.paywall_plan_trial, trialDays, trialDays)
    else -> stringResource(R.string.paywall_plan_yearly_renews)
}

@Composable
private fun ProOfferUi.secondHintLabel(): String? = when (plan) {
    ProPlan.YEARLY -> monthlyPrice?.let { stringResource(R.string.paywall_plan_yearly_monthly, it) }
    ProPlan.LIFETIME -> yearsOfYearly?.let { pluralStringResource(R.plurals.paywall_plan_lifetime_vs_yearly, it, it) }
    ProPlan.NONE -> null
}

@Composable
private fun ProOfferUi.disclosure(): String? = when {
    plan != ProPlan.YEARLY -> null
    trialDays > 0 -> stringResource(R.string.paywall_no_charge) + "\n" +
        pluralStringResource(R.plurals.paywall_trial_disclosure, trialDays, trialDays, price)
    else -> stringResource(R.string.paywall_yearly_disclosure, price)
}

@Composable
private fun ProOfferUi?.ctaLabel(): String = when {
    this == null -> stringResource(R.string.paywall_cta_trial)
    plan == ProPlan.YEARLY && trialDays > 0 -> pluralStringResource(R.plurals.paywall_cta_trial_days, trialDays, trialDays)
    plan == ProPlan.YEARLY -> stringResource(R.string.paywall_cta_subscribe)
    else -> stringResource(R.string.paywall_cta_lifetime)
}

private fun ProPlan.titleRes(): Int =
    if (this == ProPlan.LIFETIME) R.string.paywall_plan_lifetime else R.string.paywall_plan_yearly

private fun ProFeature.headlineRes(): Int = when (this) {
    ProFeature.GENERAL -> R.string.paywall_title
    ProFeature.CUSTOM_STYLES -> R.string.paywall_context_styles
    ProFeature.DOCUMENT_EXPORT -> R.string.paywall_context_export
    ProFeature.SMART_SUGGESTIONS -> R.string.paywall_context_suggestions
    ProFeature.AUDIO_IMPORT -> R.string.paywall_context_import
}

private fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (notFound: ActivityNotFoundException) {
        Toast.makeText(context, R.string.paywall_no_browser, Toast.LENGTH_SHORT).show()
    }
}

private val COLUMN_WIDTH = 56.dp
private const val FREE_ROW_COUNT = 5
private const val SUCCESS_DISMISS_MS = 1600L
