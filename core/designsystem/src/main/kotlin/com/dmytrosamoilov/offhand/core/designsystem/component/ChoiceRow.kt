package com.dmytrosamoilov.offhand.core.designsystem.component

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

@Composable
fun ChoiceRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    hint: String? = null,
    showProBadge: Boolean = false,
    showDivider: Boolean = true,
    minHeight: Dp = 60.dp,
    role: Role = Role.RadioButton,
    trailing: @Composable () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .selectable(selected = isSelected, role = role, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ChoiceMark(isSelected = isSelected)
            Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = title,
                        style = OffhandTheme.type.body,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = OffhandTheme.colors.ink,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (showProBadge) ProBadge()
                }
                subtitle?.let { Text(text = it, style = OffhandTheme.type.caption, color = OffhandTheme.colors.ink2) }
            }
            hint?.let { Text(text = it, style = OffhandTheme.type.number.copy(fontSize = OffhandTheme.type.meta.fontSize * 12 / 11), color = OffhandTheme.colors.ink3) }
            trailing()
        }
        if (showDivider) Hairline()
    }
}

@Composable
fun ChoiceMark(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    selectedColor: Color = OffhandTheme.colors.ink,
) {
    val colors = OffhandTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.background(selectedColor)
                } else {
                    Modifier.border(1.5.dp, colors.ink3.copy(alpha = UNSELECTED_RING_ALPHA), CircleShape)
                },
            ),
    ) {
        if (isSelected) {
            Icon(OffhandIcons.Check, contentDescription = null, tint = colors.onInk, modifier = Modifier.size(size * 0.55f))
        }
    }
}

private const val UNSELECTED_RING_ALPHA = 0.6f
