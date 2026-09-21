package com.dmytrosamoilov.offhand.feature.notes.domain.export

object MarkdownBlockParser {

    private val HEADING = Regex("^#{1,6}\\s+(.*)$")
    private val BULLET = Regex("^\\s*[-*•]\\s+(.*)$")
    private val NUMBERED = Regex("^\\s*(\\d+)[.)]\\s+(.*)$")
    private val STRONG = Regex("\\*\\*(.+?)\\*\\*|__(.+?)__")
    private val EMPHASIS = Regex("(?<![\\w*])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![\\w*])|(?<!\\w)_(?!\\s)(.+?)(?<!\\s)_(?!\\w)")
    private val CODE = Regex("`([^`]*)`")

    fun parse(markdown: String): List<DocumentBlock> {
        val blocks = mutableListOf<DocumentBlock>()
        val paragraph = StringBuilder()
        fun flush() {
            if (paragraph.isNotBlank()) blocks += DocumentBlock.Paragraph(paragraph.toString().trim())
            paragraph.clear()
        }
        markdown.lines().forEach { line ->
            val block = lineBlock(line)
            when {
                line.isBlank() -> flush()
                block != null -> {
                    flush()
                    blocks += block
                }
                else -> paragraph.append(if (paragraph.isEmpty()) "" else " ").append(inline(line.trim()))
            }
        }
        flush()
        return blocks
    }

    private fun lineBlock(line: String): DocumentBlock? {
        HEADING.matchEntire(line)?.let { return DocumentBlock.Heading(inline(it.groupValues[1])) }
        BULLET.matchEntire(line)?.let { return DocumentBlock.Bullet(inline(it.groupValues[1])) }
        NUMBERED.matchEntire(line)?.let { return DocumentBlock.Numbered(it.groupValues[1].toInt(), inline(it.groupValues[2])) }
        return null
    }

    fun inline(text: String): String = text
        .replace(STRONG) { it.groupValues[1].ifEmpty { it.groupValues[2] } }
        .replace(EMPHASIS) { it.groupValues[1].ifEmpty { it.groupValues[2] } }
        .replace(CODE) { it.groupValues[1] }
        .trim()
}
