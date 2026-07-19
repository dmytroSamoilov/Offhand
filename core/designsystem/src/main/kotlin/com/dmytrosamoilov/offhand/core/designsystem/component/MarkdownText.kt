package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        markdown.lines()
            .map(String::trimEnd)
            .filter(String::isNotBlank)
            .forEach { line -> MarkdownLine(line) }
    }
}

@Composable
private fun MarkdownLine(line: String) {
    val trimmed = line.trimStart()
    when {
        trimmed.startsWith("### ") -> HeadingText(trimmed.removePrefix("### "), MaterialTheme.typography.titleMedium)
        trimmed.startsWith("## ") -> HeadingText(trimmed.removePrefix("## "), MaterialTheme.typography.titleLarge)
        trimmed.startsWith("# ") -> HeadingText(trimmed.removePrefix("# "), MaterialTheme.typography.headlineSmall)
        trimmed.startsWith("- ") -> BulletText(trimmed.removePrefix("- "))
        trimmed.startsWith("* ") -> BulletText(trimmed.removePrefix("* "))
        NUMBERED_ITEM.matches(trimmed) -> BulletText(trimmed)
        else -> Text(
            text = stripInlineMarkers(trimmed),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun HeadingText(text: String, style: TextStyle) {
    Text(text = stripInlineMarkers(text), style = style)
}

@Composable
private fun BulletText(text: String) {
    Row {
        Text(text = "•", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stripInlineMarkers(text),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private val NUMBERED_ITEM = Regex("^\\d+\\.\\s.*")
private val BOLD_MARKER = Regex("\\*\\*(.+?)\\*\\*")
private val ITALIC_MARKER = Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)")

private fun stripInlineMarkers(text: String): String = text
    .replace(BOLD_MARKER, "$1")
    .replace(ITALIC_MARKER, "$1")
    .replace("`", "")
