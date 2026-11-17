package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val blocks = markdown.lines()
        .map(String::trim)
        .filter(String::isNotBlank)
        .map(::toBlock)
    SelectionContainer(modifier = modifier) {
        Column {
            blocks.forEachIndexed { index, block ->
                MarkdownBlockView(block = block, modifier = Modifier.padding(top = block.spacingAbove(blocks.getOrNull(index - 1))))
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Heading(val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Numbered(val number: String, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

private fun toBlock(line: String): MarkdownBlock {
    val numbered = NUMBERED_ITEM.matchEntire(line)
    return when {
        HEADING.matches(line) -> MarkdownBlock.Heading(stripInlineMarkers(line.trimStart('#').trim()))
        line.startsWith("- ") || line.startsWith("* ") -> MarkdownBlock.Bullet(stripInlineMarkers(line.drop(2)))
        numbered != null -> MarkdownBlock.Numbered(numbered.groupValues[1], stripInlineMarkers(numbered.groupValues[2]))
        else -> MarkdownBlock.Paragraph(stripInlineMarkers(line))
    }
}

private fun MarkdownBlock.spacingAbove(previous: MarkdownBlock?) = when {
    previous == null -> 0.dp
    this is MarkdownBlock.Heading -> HEADING_SPACING
    previous is MarkdownBlock.Heading -> LABEL_SPACING
    else -> ITEM_SPACING
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock, modifier: Modifier) {
    when (block) {
        is MarkdownBlock.Heading -> SectionLabel(text = block.text, modifier = modifier.semantics { heading() })
        is MarkdownBlock.Bullet -> ListItemRow(modifier = modifier, text = block.text) { BulletDot() }
        is MarkdownBlock.Numbered -> ListItemRow(modifier = modifier, text = block.text) { ItemNumber(block.number) }
        is MarkdownBlock.Paragraph -> BodyText(text = block.text, modifier = modifier)
    }
}

@Composable
private fun ListItemRow(modifier: Modifier, text: String, marker: @Composable () -> Unit) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        marker()
        BodyText(text = text)
    }
}

@Composable
private fun BulletDot() {
    Box(
        modifier = Modifier
            .padding(top = 10.dp)
            .size(5.dp)
            .background(OffhandTheme.colors.ink, CircleShape),
    )
}

@Composable
private fun ItemNumber(number: String) {
    Text(text = "$number.", style = OffhandTheme.type.body, color = OffhandTheme.colors.ink2)
}

@Composable
private fun BodyText(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = OffhandTheme.type.body, color = OffhandTheme.colors.ink, modifier = modifier)
}

private val HEADING_SPACING = 24.dp
private val LABEL_SPACING = 10.dp
private val ITEM_SPACING = 8.dp
private val HEADING = Regex("^#{1,6}\\s.*")
private val NUMBERED_ITEM = Regex("^(\\d+)\\.\\s+(.*)")
private val BOLD_MARKER = Regex("\\*\\*(.+?)\\*\\*")
private val ITALIC_MARKER = Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)")

private fun stripInlineMarkers(text: String): String = text
    .replace(BOLD_MARKER, "$1")
    .replace(ITALIC_MARKER, "$1")
    .replace("`", "")
