package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.component.BarIconButton
import com.dmytrosamoilov.offhand.core.designsystem.component.ChoiceRow
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.SurfaceCard
import com.dmytrosamoilov.offhand.core.designsystem.component.TopBar
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.core.ui.component.ReadableSectionLabel

@Composable
internal fun SettingsSection(
    label: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        label?.let { ReadableSectionLabel(text = it, modifier = Modifier.padding(start = 8.dp, top = 28.dp, bottom = 10.dp)) }
        SurfaceCard(content = content)
    }
}

@Composable
internal fun SettingsFootnote(text: String) {
    Text(
        text = text,
        style = OffhandTheme.type.caption,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
    )
}

@Composable
internal fun SettingsChoiceRow(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    ChoiceRow(
        title = title,
        subtitle = subtitle,
        isSelected = isSelected,
        onClick = onClick,
        showDivider = showDivider,
        modifier = Modifier.padding(horizontal = OffhandSpacing.card),
    )
}

@Composable
internal fun BackTopBar(
    backDescription: String,
    onBack: () -> Unit,
    title: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    TopBar(
        title = title,
        leading = { BarIconButton(icon = OffhandIcons.Back, contentDescription = backDescription, onClick = onBack) },
        trailing = { trailing() },
    )
}

@Composable
internal fun SettingsPage(
    topBar: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        topBar()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OffhandSpacing.screen)
                .navigationBarsPadding(),
            content = {
                content()
                Spacer(modifier = Modifier.height(32.dp))
            },
        )
    }
}
