package com.dmytrosamoilov.offhand.feature.paywall.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.common.LegalLinks
import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.designsystem.component.BarIconButton
import com.dmytrosamoilov.offhand.core.designsystem.component.GoldButton
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandDialog
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.PlainTextButton
import com.dmytrosamoilov.offhand.core.designsystem.component.ProCrown
import com.dmytrosamoilov.offhand.core.designsystem.component.SecondaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.TextButtonTone
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandDarkContent
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.paywall.R
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun PaywallScreen(viewModel: PaywallViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onOpened() }
    LaunchedEffect(state.isPro, state.mode) {
        if (state.isPro && state.mode == PaywallMode.OFFER) {
            delay(SUCCESS_DISMISS_MS)
            viewModel.onClosed()
        }
    }
    BackHandler(onBack = viewModel::onClosed)

    OffhandDarkContent {
        Box(modifier = Modifier.fillMaxSize().background(OffhandTheme.colors.ground)) {
            when {
                state.mode == PaywallMode.OPENING -> Unit
                state.mode == PaywallMode.BENEFITS -> BenefitsContent(isSummaryAvailable = state.isSummaryAvailable, onClose = viewModel::onClosed)
                state.isPro -> SuccessContent(feature = state.feature)
                else -> OfferContent(state = state, viewModel = viewModel)
            }
        }
        state.message?.let { message ->
            PaywallMessageDialog(message = message, onDismiss = viewModel::onMessageDismissed)
        }
    }
}

@Composable
private fun OfferContent(state: PaywallUiState, viewModel: PaywallViewModel) {
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        PaywallTopBar(onClose = viewModel::onClosed) {
            PlainTextButton(
                text = stringResource(R.string.paywall_restore),
                onClick = viewModel::onRestoreClicked,
                tone = TextButtonTone.Muted,
                enabled = !state.isPurchasing,
            )
        }
        ScrollingBody {
            OfferHeader(state = state)
            if (state.shownTrialRecap.isNotEmpty()) TrialRecap(lines = state.shownTrialRecap)
            BenefitLines(highlighted = state.feature, isSummaryAvailable = state.isSummaryAvailable)
            PlanSection(state = state, onPlanSelected = viewModel::onPlanSelected, onRetry = viewModel::onRetryOffers)
            ComparisonTable(highlighted = state.feature, isSummaryAvailable = state.isSummaryAvailable, isCollapsible = true)
            LegalLinksRow()
        }
        if (state.feature == ProFeature.FREE_TRIAL) {
            TrialIntroActions(state = state, onStartTrial = viewModel::onClosed, onPurchase = viewModel::onPurchaseClicked)
        } else {
            OfferActions(state = state, onPurchase = viewModel::onPurchaseClicked, onContinueFree = viewModel::onClosed)
        }
    }
}

@Composable
private fun PaywallTopBar(onClose: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BarIconButton(icon = OffhandIcons.Close, contentDescription = stringResource(R.string.paywall_close), onClick = onClose)
        trailing()
    }
}

@Composable
private fun ColumnScope.ScrollingBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = OffhandSpacing.screen)
            .padding(bottom = OffhandSpacing.section),
        verticalArrangement = Arrangement.spacedBy(OffhandSpacing.section),
        content = content,
    )
}

@Composable
private fun OfferHeader(state: PaywallUiState) {
    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProLabel()
        Text(text = state.headline(), style = OffhandTheme.type.title, color = OffhandTheme.colors.ink)
        Text(text = stringResource(state.taglineRes()), style = OffhandTheme.type.body, color = OffhandTheme.colors.ink2)
    }
}

@Composable
private fun LegalLinksRow() {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        PlainTextButton(
            text = stringResource(R.string.paywall_terms),
            onClick = { openLink(context, LegalLinks.TERMS) },
            tone = TextButtonTone.Muted,
        )
        PlainTextButton(
            text = stringResource(R.string.paywall_privacy),
            onClick = { openLink(context, LegalLinks.PRIVACY_POLICY) },
            tone = TextButtonTone.Muted,
        )
    }
}

@Composable
private fun OfferActions(state: PaywallUiState, onPurchase: () -> Unit, onContinueFree: () -> Unit) {
    val offer = state.selectedOffer
    PinnedActions {
        GoldButton(
            text = offer.ctaLabel(),
            onClick = onPurchase,
            enabled = offer != null && !state.isPurchasing,
            modifier = Modifier.fillMaxWidth(),
        )
        PlainTextButton(
            text = stringResource(R.string.paywall_continue_free),
            onClick = onContinueFree,
            tone = TextButtonTone.Muted,
            modifier = Modifier.fillMaxWidth(),
        )
        offer?.disclosure()?.let { Disclosure(text = it) }
    }
}

@Composable
private fun TrialIntroActions(state: PaywallUiState, onStartTrial: () -> Unit, onPurchase: () -> Unit) {
    val offer = state.selectedOffer
    PinnedActions {
        GoldButton(
            text = stringResource(R.string.paywall_trial_offer_continue),
            onClick = onStartTrial,
            enabled = !state.isPurchasing,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = stringResource(offer.buyNowLabelRes()),
            onClick = onPurchase,
            enabled = offer != null && !state.isPurchasing,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
private fun PinnedActions(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
private fun Disclosure(text: String) {
    Text(
        text = text,
        style = OffhandTheme.type.caption.copy(fontSize = OffhandTheme.type.meta.fontSize * 12 / 11),
        color = OffhandTheme.colors.ink3,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun BenefitsContent(isSummaryAvailable: Boolean, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        PaywallTopBar(onClose = onClose)
        ScrollingBody {
            Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProLabel()
                Text(
                    text = stringResource(R.string.paywall_success_title),
                    style = OffhandTheme.type.title,
                    color = OffhandTheme.colors.ink,
                )
                Text(
                    text = stringResource(R.string.paywall_benefits_body),
                    style = OffhandTheme.type.body,
                    color = OffhandTheme.colors.ink2,
                )
            }
            ComparisonTable(highlighted = ProFeature.GENERAL, isSummaryAvailable = isSummaryAvailable, isCollapsible = false)
        }
    }
}

@Composable
private fun SuccessContent(feature: ProFeature) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        ProCrown(size = 56.dp)
        Text(
            text = stringResource(R.string.paywall_success_title),
            style = OffhandTheme.type.title,
            color = OffhandTheme.colors.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(if (feature == ProFeature.GENERAL) R.string.paywall_success_body else R.string.paywall_success_continue),
            style = OffhandTheme.type.body,
            color = OffhandTheme.colors.ink2,
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
    OffhandDialog(
        title = stringResource(R.string.paywall_title),
        body = stringResource(text),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.paywall_message_dismiss),
        onConfirm = onDismiss,
    )
}

private fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (notFound: ActivityNotFoundException) {
        Toast.makeText(context, R.string.paywall_no_browser, Toast.LENGTH_SHORT).show()
    }
}

private const val SUCCESS_DISMISS_MS = 1600L

@Composable
private fun PaywallUiState.headline(): String {
    val daysLeft = trialDaysLeft
    return if (feature == ProFeature.FREE_TRIAL && daysLeft != null) {
        pluralStringResource(R.plurals.paywall_trial_offer_title, daysLeft, daysLeft)
    } else {
        stringResource(headlineRes())
    }
}
