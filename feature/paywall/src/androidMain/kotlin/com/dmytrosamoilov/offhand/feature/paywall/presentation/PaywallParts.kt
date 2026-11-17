package com.dmytrosamoilov.offhand.feature.paywall.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsageCount
import com.dmytrosamoilov.offhand.core.designsystem.component.ChoiceMark
import com.dmytrosamoilov.offhand.core.designsystem.component.Hairline
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.PlainTextButton
import com.dmytrosamoilov.offhand.core.designsystem.component.ProCrown
import com.dmytrosamoilov.offhand.core.designsystem.component.SurfaceCard
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.paywall.R

@Composable
internal fun ProLabel() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ProCrown(size = 22.dp, modifier = Modifier.clearAndSetSemantics { })
        Text(
            text = stringResource(R.string.paywall_title).uppercase(),
            style = OffhandTheme.type.label.copy(letterSpacing = 0.12.em),
            color = OffhandTheme.colors.gold,
        )
    }
}

@Composable
internal fun BenefitLines(highlighted: ProFeature, isSummaryAvailable: Boolean) {
    val rows = remember(highlighted, isSummaryAvailable) {
        sortedRows(highlighted, isSummaryAvailable).filter { it.isPro }.take(BENEFIT_LINES)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoldCheck()
                Text(text = stringResource(row.titleRes), style = OffhandTheme.type.dialogBody, color = OffhandTheme.colors.ink)
            }
        }
    }
}

@Composable
private fun GoldCheck() {
    Icon(imageVector = OffhandIcons.Check, contentDescription = null, tint = OffhandTheme.colors.gold, modifier = Modifier.size(18.dp))
}

@Composable
internal fun TrialRecap(lines: List<ProTrialUsageCount>) {
    SurfaceCard {
        lines.forEachIndexed { index, line ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(line.usage.labelRes()),
                    style = OffhandTheme.type.settingTitle,
                    color = OffhandTheme.colors.ink,
                    modifier = Modifier.weight(1f),
                )
                Text(text = line.count.toString(), style = OffhandTheme.type.number, color = OffhandTheme.colors.gold)
            }
            if (index < lines.lastIndex) Hairline()
        }
    }
}

@Composable
internal fun PlanSection(state: PaywallUiState, onPlanSelected: (ProPlan) -> Unit, onRetry: () -> Unit) {
    when {
        state.isLoadingOffers -> Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = OffhandTheme.colors.ink, trackColor = OffhandTheme.colors.hairline, strokeWidth = 2.5.dp)
        }
        state.offers.isEmpty() -> OffersUnavailable(onRetry = onRetry)
        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().selectableGroup()) {
            state.offers.forEach { offer ->
                PlanRow(offer = offer, isSelected = offer.plan == state.selectedPlan, onClick = { onPlanSelected(offer.plan) })
            }
        }
    }
}

@Composable
private fun OffersUnavailable(onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.paywall_offers_unavailable),
            style = OffhandTheme.type.bodySmall,
            color = OffhandTheme.colors.ink2,
            textAlign = TextAlign.Center,
        )
        PlainTextButton(text = stringResource(R.string.paywall_retry), onClick = onRetry)
    }
}

@Composable
private fun PlanRow(offer: ProOfferUi, isSelected: Boolean, onClick: () -> Unit) {
    val colors = OffhandTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(shape)
            .background(colors.well)
            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) colors.gold else colors.border, shape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ChoiceMark(isSelected = isSelected, selectedColor = colors.gold)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PlanTitle(offer = offer)
            Text(text = offer.hints(), style = OffhandTheme.type.caption, color = colors.ink2)
        }
        Text(text = offer.priceLabel(), style = OffhandTheme.type.number.copy(fontSize = OffhandTheme.type.dialogBody.fontSize), color = colors.ink)
    }
}

@Composable
private fun PlanTitle(offer: ProOfferUi) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(offer.plan.titleRes()),
            style = OffhandTheme.type.body,
            fontWeight = FontWeight.SemiBold,
            color = OffhandTheme.colors.ink,
            maxLines = 1,
        )
        if (offer.plan == ProPlan.LIFETIME) {
            Text(
                text = stringResource(R.string.paywall_plan_best_value).uppercase(),
                style = OffhandTheme.type.meta,
                color = OffhandTheme.colors.ink2,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProOfferUi.hints(): String = listOfNotNull(hintLabel(), secondHintLabel()).joinToString(separator = " · ")

@Composable
internal fun ComparisonTable(highlighted: ProFeature, isSummaryAvailable: Boolean, isCollapsible: Boolean) {
    var isExpanded by rememberSaveable { mutableStateOf(!isCollapsible) }
    Column(modifier = Modifier.fillMaxWidth().animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (isExpanded) ComparisonCard(highlighted = highlighted, isSummaryAvailable = isSummaryAvailable)
        if (isCollapsible) {
            PlainTextButton(
                text = stringResource(if (isExpanded) R.string.paywall_benefits_collapse else R.string.paywall_benefits_expand),
                onClick = { isExpanded = !isExpanded },
            )
        }
    }
}

@Composable
private fun ComparisonCard(highlighted: ProFeature, isSummaryAvailable: Boolean) {
    val rows = remember(highlighted, isSummaryAvailable) { sortedRows(highlighted, isSummaryAvailable) }
    SurfaceCard {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            TableHeader()
            rows.forEachIndexed { index, row ->
                if (index > 0 && row.isPro != rows[index - 1].isPro) Hairline(modifier = Modifier.padding(vertical = 4.dp))
                TableRow(row = row, isHighlighted = highlighted in row.features)
            }
            TableFootnote(text = stringResource(R.string.paywall_pro_more))
            TableFootnote(text = stringResource(R.string.paywall_privacy_note))
        }
    }
}

@Composable
private fun TableHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f))
        ColumnHeading(text = stringResource(R.string.paywall_column_free), color = OffhandTheme.colors.ink2)
        ColumnHeading(text = stringResource(R.string.paywall_column_pro), color = OffhandTheme.colors.gold)
    }
}

@Composable
private fun ColumnHeading(text: String, color: Color) {
    Text(
        text = text.uppercase(),
        style = OffhandTheme.type.label,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.width(COLUMN_WIDTH),
    )
}

@Composable
private fun TableRow(row: ComparisonRow, isHighlighted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isHighlighted) OffhandTheme.colors.well else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(row.titleRes),
                style = OffhandTheme.type.bodySmall,
                fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Medium,
                color = OffhandTheme.colors.ink,
            )
            row.hintRes?.let { Text(text = stringResource(it), style = OffhandTheme.type.caption, color = OffhandTheme.colors.ink2) }
        }
        CheckCell(isIncluded = !row.isPro, tint = OffhandTheme.colors.ink2)
        CheckCell(isIncluded = true, tint = OffhandTheme.colors.gold)
    }
}

@Composable
private fun CheckCell(isIncluded: Boolean, tint: Color) {
    Box(modifier = Modifier.width(COLUMN_WIDTH), contentAlignment = Alignment.Center) {
        if (isIncluded) {
            Icon(imageVector = OffhandIcons.Check, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        } else {
            Text(text = stringResource(R.string.paywall_not_included), color = OffhandTheme.colors.ink3)
        }
    }
}

@Composable
private fun TableFootnote(text: String) {
    Text(
        text = text,
        style = OffhandTheme.type.caption,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

private val COLUMN_WIDTH = 68.dp
private const val BENEFIT_LINES = 3
