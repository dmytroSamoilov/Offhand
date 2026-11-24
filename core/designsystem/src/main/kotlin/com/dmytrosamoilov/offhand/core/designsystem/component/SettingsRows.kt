package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dmytrosamoilov.offhand.core.designsystem.R
import com.dmytrosamoilov.offhand.core.designsystem.haptics.haptics
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandMotion
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandRadius
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

@Composable
fun SettingsGroup(
    label: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        label?.let {
            SectionLabel(text = it, modifier = Modifier.padding(start = 8.dp, top = 28.dp, bottom = 10.dp))
        }
        SurfaceCard(content = content)
    }
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OffhandTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OffhandRadius.card))
            .background(colors.surface)
            .border(1.dp, colors.hairline, RoundedCornerShape(OffhandRadius.card)),
        content = content,
    )
}

@Composable
fun LinkRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    enabled: Boolean = true,
    showProBadge: Boolean = false,
    showDivider: Boolean = true,
    trailing: @Composable () -> Unit = { RowChevron() },
) {
    SettingsRowLayout(
        title = title,
        subtitle = subtitle,
        showProBadge = showProBadge,
        showDivider = showDivider,
        modifier = modifier
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
    ) {
        value?.let { Text(text = it, style = OffhandTheme.type.bodySmall, color = OffhandTheme.colors.ink2) }
        trailing()
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    showProBadge: Boolean = false,
    showDivider: Boolean = true,
) {
    val haptics = haptics()
    SettingsRowLayout(
        title = title,
        subtitle = subtitle,
        showProBadge = showProBadge,
        showDivider = showDivider,
        modifier = modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = { isOn ->
                    haptics.toggle(isOn)
                    onCheckedChange(isOn)
                },
            )
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
    ) {
        OffhandSwitch(checked = checked)
    }
}

@Composable
fun SettingsRowLayout(
    title: String,
    subtitle: String?,
    showProBadge: Boolean,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 62.dp)
                .padding(horizontal = OffhandSpacing.card),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OffhandSpacing.controls),
        ) {
            Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = title,
                        style = OffhandTheme.type.settingTitle,
                        color = OffhandTheme.colors.ink,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (showProBadge) ProBadge()
                }
                subtitle?.let { Text(text = it, style = OffhandTheme.type.caption, color = OffhandTheme.colors.ink2) }
            }
            trailing()
        }
        if (showDivider) Hairline()
    }
}

@Composable
fun RowChevron() {
    Icon(
        imageVector = OffhandIcons.ChevronRight,
        contentDescription = null,
        tint = OffhandTheme.colors.ink3,
        modifier = Modifier.size(16.dp),
    )
}

@Composable
fun OffhandSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = OffhandTheme.colors
    val track by animateColorAsState(if (checked) colors.ink else colors.border, OffhandMotion.state(), label = "switch")
    val knobOffset by animateDpAsState(if (checked) 23.dp else 3.dp, OffhandMotion.state(), label = "switch-knob")
    Box(
        modifier = modifier
            .size(48.dp, 28.dp)
            .clip(CircleShape)
            .background(track),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(22.dp)
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(switchKnob(checked)),
        )
    }
}

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    val colors = OffhandTheme.colors
    Text(
        text = stringResource(R.string.designsystem_pro_badge_label),
        style = OffhandTheme.type.label.copy(fontSize = 10.sp, letterSpacing = 0.08.em),
        fontWeight = FontWeight.Bold,
        color = colors.onGold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.gold)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
private fun switchKnob(checked: Boolean) = OffhandTheme.colors.let { colors ->
    when {
        !colors.isDark -> colors.surface
        checked -> colors.onInk
        else -> colors.ink2
    }
}
