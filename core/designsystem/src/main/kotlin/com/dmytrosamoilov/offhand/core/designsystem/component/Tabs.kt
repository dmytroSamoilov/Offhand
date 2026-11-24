package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandMotion
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

data class TabItem(
    val label: String,
    val dotDescription: String? = null,
)

@Composable
fun UnderlineTabs(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 0.dp,
    spacing: Dp = 24.dp,
    tabModifier: @Composable (Int) -> Modifier = { Modifier },
    trailing: @Composable () -> Unit = {},
) {
    val hairline = OffhandTheme.colors.hairline
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(hairline, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx())
            }
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = contentPadding)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            UnderlineTab(
                tab = tab,
                isSelected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = tabModifier(index),
            )
        }
        trailing()
    }
}

@Composable
fun UnderlineTab(
    tab: TabItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OffhandTheme.colors
    val textColor by animateColorAsState(if (isSelected) colors.ink else colors.ink2, OffhandMotion.state(), label = "tab")
    val accent = colors.accent
    Row(
        modifier = modifier
            .height(OffhandSpacing.touchTarget)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .then(tab.dotDescription?.let { description -> Modifier.semantics { stateDescription = description } } ?: Modifier)
            .drawBehind {
                if (isSelected) {
                    val stroke = 2.dp.toPx()
                    drawLine(accent, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = tab.label,
            style = OffhandTheme.type.tab,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
        )
        if (tab.dotDescription != null) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
        }
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = OffhandTheme.colors
    val outer = if (compact) 14.dp else 16.dp
    val inner = if (compact) 10.dp else 12.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(outer))
            .background(colors.well)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            val background by animateColorAsState(
                if (isSelected) colors.wellSelected else colors.well,
                OffhandMotion.state(),
                label = "segment",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 40.dp else OffhandSpacing.touchTarget)
                    .then(if (isSelected) Modifier.shadow(1.dp, RoundedCornerShape(inner)) else Modifier)
                    .clip(RoundedCornerShape(inner))
                    .background(background)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) }),
            ) {
                Text(
                    text = option,
                    style = if (compact) OffhandTheme.type.bodySmall else OffhandTheme.type.tab,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) colors.ink else colors.ink2,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, isEmphasised: Boolean = false) {
    Text(
        text = text.uppercase(),
        style = OffhandTheme.type.label,
        color = if (isEmphasised) OffhandTheme.colors.ink else OffhandTheme.colors.ink3,
        modifier = modifier,
    )
}

@Composable
fun MetaText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = OffhandTheme.type.meta,
        color = OffhandTheme.colors.ink3,
        modifier = modifier,
        maxLines = 1,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(OffhandTheme.colors.hairline))
}

@Composable
fun ProgressLine(
    progress: Float?,
    modifier: Modifier = Modifier,
    thickness: Dp = 2.dp,
) {
    val colors = OffhandTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .clip(CircleShape)
            .background(colors.hairline),
    ) {
        Box(
            Modifier
                .fillMaxWidth((progress ?: INDETERMINATE_FRACTION).coerceIn(0f, 1f))
                .height(thickness)
                .background(colors.accent),
        )
    }
}

private const val INDETERMINATE_FRACTION = 0.08f
