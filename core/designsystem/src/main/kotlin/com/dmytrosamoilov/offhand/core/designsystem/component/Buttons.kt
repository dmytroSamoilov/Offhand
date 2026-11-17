package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

enum class TextButtonTone { Accent, Ink, Muted, Danger }

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = OffhandSpacing.control,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = OffhandTheme.colors
    PillSurface(
        onClick = onClick,
        enabled = enabled,
        container = colors.ink,
        border = null,
        modifier = modifier.height(height),
    ) {
        ButtonContent(text = text, icon = icon, color = colors.onInk, trailing = trailing)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = OffhandSpacing.control,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = OffhandTheme.colors
    PillSurface(
        onClick = onClick,
        enabled = enabled,
        container = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier.height(height),
    ) {
        ButtonContent(text = text, icon = icon, color = colors.ink, trailing = trailing)
    }
}

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = OffhandTheme.colors
    PillSurface(
        onClick = onClick,
        enabled = enabled,
        container = colors.gold,
        border = null,
        modifier = modifier.height(OffhandSpacing.control),
    ) {
        ButtonContent(text = text, icon = null, color = colors.onGold, trailing = {})
    }
}

@Composable
fun PlainTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: TextButtonTone = TextButtonTone.Accent,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = OffhandTheme.colors
    val color = when (tone) {
        TextButtonTone.Accent -> colors.accent
        TextButtonTone.Ink -> colors.ink
        TextButtonTone.Muted -> colors.ink2
        TextButtonTone.Danger -> colors.danger
    }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = OffhandSpacing.touchTarget)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = OffhandSpacing.controls),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OffhandSpacing.related, Alignment.CenterHorizontally),
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(18.dp)) }
        Text(text = text, style = OffhandTheme.type.buttonSmall, color = color)
    }
}

@Composable
fun RecordPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    height: Dp = OffhandSpacing.control,
) {
    val colors = OffhandTheme.colors
    PillSurface(
        onClick = onClick,
        enabled = true,
        container = colors.ink,
        border = null,
        modifier = modifier
            .shadow(
                elevation = if (colors.isDark) 0.dp else 12.dp,
                shape = CircleShape,
                ambientColor = colors.ink,
                spotColor = colors.ink,
            )
            .height(height)
            .then(contentDescription?.let { description -> Modifier.semantics { this.contentDescription = description } } ?: Modifier),
        contentPadding = PaddingValues(start = 24.dp, end = 28.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(colors.record))
        Text(text = text, style = OffhandTheme.type.button, color = colors.onInk, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
fun PausePill(
    text: String,
    isPaused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SecondaryButton(
        text = text,
        onClick = onClick,
        icon = if (isPaused) OffhandIcons.Play else OffhandIcons.Pause,
        modifier = modifier,
    )
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = OffhandSpacing.control,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = OffhandTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) colors.ink else colors.surface)
            .then(if (filled) Modifier else Modifier.border(1.dp, colors.border, CircleShape))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (filled) colors.onInk else colors.ink,
            modifier = Modifier.size(if (size > 48.dp) 20.dp else 18.dp),
        )
    }
}

@Composable
fun BarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = OffhandTheme.colors.ink,
    enabled: Boolean = true,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(OffhandSpacing.touchTarget)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun PillSurface(
    onClick: () -> Unit,
    enabled: Boolean,
    container: Color,
    border: BorderStroke?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = OffhandSpacing.screen),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .then(border?.let { Modifier.border(it, CircleShape) } ?: Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    icon: ImageVector?,
    color: Color,
    trailing: @Composable RowScope.() -> Unit,
) {
    icon?.let {
        Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Box(Modifier.size(10.dp))
    }
    Text(
        text = text,
        style = OffhandTheme.type.button,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    trailing()
}

internal const val DISABLED_ALPHA = 0.4f
