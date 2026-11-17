package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

enum class BottomBarDestination { Notes, Settings }

data class BottomBarLabels(
    val notes: String,
    val settings: String,
    val record: String,
    val recordDescription: String,
)

@Composable
fun BottomBar(
    selected: BottomBarDestination,
    labels: BottomBarLabels,
    onNotes: () -> Unit,
    onRecord: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    notesModifier: Modifier = Modifier,
    settingsModifier: Modifier = Modifier,
) {
    val colors = OffhandTheme.colors
    BoxWithConstraints(modifier = modifier.fillMaxWidth().background(colors.ground)) {
        val sidePadding = if (maxWidth < COMPACT_BAR_WIDTH) COMPACT_SIDE_PADDING else SIDE_PADDING
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(BAR_HEIGHT)
                .padding(horizontal = sidePadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BarItem(
                label = labels.notes,
                icon = OffhandIcons.Notes,
                isSelected = selected == BottomBarDestination.Notes,
                onClick = onNotes,
                modifier = notesModifier,
            )
            RecordPill(text = labels.record, contentDescription = labels.recordDescription, onClick = onRecord)
            BarItem(
                label = labels.settings,
                icon = OffhandIcons.Settings,
                isSelected = selected == BottomBarDestination.Settings,
                onClick = onSettings,
                modifier = settingsModifier,
            )
        }
    }
}

@Composable
private fun BarItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OffhandTheme.colors
    val tint = if (isSelected) colors.ink else colors.ink2
    Column(
        modifier = modifier
            .size(64.dp, 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            style = OffhandTheme.type.label.copy(letterSpacing = 0.sp),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    linkLabel: String? = null,
    onLink: () -> Unit = {},
) {
    val colors = OffhandTheme.colors
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.padding(bottom = 4.dp)) { WaveGlyph(seed = EMPTY_WAVE_SEED) }
        Text(
            text = title,
            style = OffhandTheme.type.emptyTitle,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        body?.let {
            Text(
                text = it,
                style = OffhandTheme.type.body.copy(lineHeight = 24.sp),
                color = colors.ink2,
                textAlign = TextAlign.Center,
            )
        }
        linkLabel?.let { PlainTextButton(text = it, onClick = onLink, modifier = Modifier.padding(top = 8.dp)) }
    }
}

private val BAR_HEIGHT = 84.dp
private val COMPACT_BAR_WIDTH = 360.dp
private val SIDE_PADDING = 28.dp
private val COMPACT_SIDE_PADDING = 8.dp
private const val EMPTY_WAVE_SEED = 7L
