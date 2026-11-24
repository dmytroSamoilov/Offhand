package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandRadius
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.settings.R

@Composable
internal fun PlanCard(status: ProStatusUi, onClick: () -> Unit) {
    val isOffer = status == ProStatusUi.Free || status is ProStatusUi.AppTrial
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OffhandRadius.card))
            .background(OffhandTheme.colors.ink)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = if (isOffer) 4.dp else 18.dp),
    ) {
        PlanTitleRow(status = status)
        if (status is ProStatusUi.AppTrial) GoldProgress(fraction = status.elapsed)
        Text(
            text = status.statusLabel(),
            style = OffhandTheme.type.caption,
            color = OffhandTheme.colors.onInk.copy(alpha = BODY_ALPHA),
            modifier = Modifier.padding(top = 12.dp),
        )
        if (isOffer) UpgradeAction(onClick = onClick)
    }
}

@Composable
private fun PlanTitleRow(status: ProStatusUi) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.settings_pro_title),
            style = OffhandTheme.type.heading.copy(fontSize = 20.sp, lineHeight = 26.sp),
            color = OffhandTheme.colors.onInk,
            modifier = Modifier.weight(1f),
        )
        when (status) {
            is ProStatusUi.AppTrial -> Text(
                text = pluralStringResource(R.plurals.settings_plan_days_left, status.daysLeft, status.daysLeft).uppercase(),
                style = OffhandTheme.type.number.copy(fontSize = 12.sp),
                color = OffhandTheme.colors.gold,
            )
            ProStatusUi.Free -> Unit
            else -> Icon(
                imageVector = OffhandIcons.ChevronRight,
                contentDescription = null,
                tint = OffhandTheme.colors.onInk.copy(alpha = BODY_ALPHA),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun GoldProgress(fraction: Float) {
    Box(
        modifier = Modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .height(3.dp)
            .clip(CircleShape)
            .background(OffhandTheme.colors.onInk.copy(alpha = TRACK_ALPHA)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(3.dp)
                .background(OffhandTheme.colors.gold),
        )
    }
}

@Composable
private fun UpgradeAction(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(end = OffhandSpacing.controls),
    ) {
        Text(
            text = stringResource(R.string.settings_pro_upgrade),
            style = OffhandTheme.type.buttonSmall,
            color = OffhandTheme.colors.gold,
        )
    }
}

private const val BODY_ALPHA = 0.78f
private const val TRACK_ALPHA = 0.18f
