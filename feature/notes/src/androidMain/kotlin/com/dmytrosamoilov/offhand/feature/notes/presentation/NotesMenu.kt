package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.component.ProBadge
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandRadius
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

@Composable
internal fun NotesMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OffhandTheme.colors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(OffhandRadius.field),
        containerColor = colors.surface,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, colors.hairline),
        content = content,
    )
}

@Composable
internal fun NotesMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDanger: Boolean = false,
    subtitle: String? = null,
    showProBadge: Boolean = false,
) {
    val color = if (isDanger) OffhandTheme.colors.danger else OffhandTheme.colors.ink
    DropdownMenuItem(
        text = { MenuItemText(text = text, subtitle = subtitle, showProBadge = showProBadge, isDanger = isDanger) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp)) },
        onClick = onClick,
    )
}

@Composable
private fun MenuItemText(text: String, subtitle: String?, showProBadge: Boolean, isDanger: Boolean) {
    val colors = OffhandTheme.colors
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = text, style = OffhandTheme.type.settingTitle, color = if (isDanger) colors.danger else colors.ink)
            if (showProBadge) ProBadge()
        }
        subtitle?.let { Text(text = it, style = OffhandTheme.type.caption, color = colors.ink2) }
    }
}
