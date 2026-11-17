package com.dmytrosamoilov.offhand.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandTextField
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

data class FieldLines(val min: Int = 1, val max: Int = 1) {
    val isSingleLine: Boolean get() = max == 1
}

@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    help: String? = null,
    lines: FieldLines = FieldLines(),
    enabled: Boolean = true,
    filledWithGround: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    focusesOnAppear: Boolean = false,
    trailing: @Composable () -> Unit = {},
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(focusesOnAppear) {
        if (focusesOnAppear) focus.requestFocus()
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = OffhandTheme.type.caption,
            fontWeight = FontWeight.SemiBold,
            color = OffhandTheme.colors.ink,
            modifier = Modifier.clickable(interactionSource = null, indication = null) { focus.requestFocus() },
        )
        OffhandTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.focusRequester(focus),
            placeholder = placeholder,
            error = error,
            singleLine = lines.isSingleLine,
            minLines = lines.min,
            maxLines = lines.max,
            enabled = enabled,
            filledWithGround = filledWithGround,
            keyboardType = keyboardType,
            visualTransformation = visualTransformation,
            trailing = trailing,
        )
        if (error == null) help?.let { Text(text = it, style = OffhandTheme.type.caption, color = OffhandTheme.colors.ink2) }
    }
}
