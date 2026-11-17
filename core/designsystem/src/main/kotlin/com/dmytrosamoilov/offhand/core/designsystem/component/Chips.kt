package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

@Composable
fun Chip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: @Composable () -> Unit = {},
) {
    val colors = OffhandTheme.colors
    Row(
        modifier = modifier
            .height(OffhandSpacing.touchTarget)
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.border, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = colors.ink, modifier = Modifier.size(16.dp)) }
        Text(text = text, style = OffhandTheme.type.bodySmall, fontWeight = FontWeight.Medium, color = colors.ink)
        trailing()
    }
}

@Composable
fun TimeMark(
    text: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    val colors = OffhandTheme.colors
    Text(
        text = text,
        style = OffhandTheme.type.number.copy(fontSize = OffhandTheme.type.number.fontSize * 12 / 13),
        color = colors.ink,
        modifier = modifier
            .clip(CircleShape)
            .background(if (isActive) colors.surface else colors.well)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}
