package com.dmytrosamoilov.offhand.feature.notes.domain.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// A4 at 72 dpi; text is laid out with StaticLayout and split across pages
// line by line, and the source footer sits at the bottom of the last page.
class AndroidNotePdfRenderer : NotePdfRenderer {

    override suspend fun render(document: NoteDocument, outputPath: String) = withContext(Dispatchers.IO) {
        val pdf = PdfDocument()
        try {
            val pages = Paginator(pdf)
            pages.draw(document.title, TITLE)
            document.details.forEach { pages.draw("${it.label}: ${it.value}", DETAIL) }
            document.sections.forEach { section ->
                pages.space(SECTION_GAP)
                pages.draw(section.heading, HEADING)
                section.blocks.forEach { pages.draw(it) }
            }
            pages.drawFooter(document.footer)
            pages.finish()
            File(outputPath).outputStream().use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }

    private class Paginator(private val pdf: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private var y = 0f
        private var pageNumber = 0

        fun draw(block: DocumentBlock) = when (block) {
            is DocumentBlock.Heading -> draw(block.text, SUBHEADING)
            is DocumentBlock.Paragraph -> draw(block.text, BODY)
            is DocumentBlock.Bullet -> draw("•  ${block.text}", BODY, indent = LIST_INDENT)
            is DocumentBlock.Numbered -> draw("${block.number}.  ${block.text}", BODY, indent = LIST_INDENT)
        }

        fun draw(text: String, paint: TextPaint, indent: Float = 0f) {
            if (text.isBlank()) return
            val width = (CONTENT_WIDTH - indent).toInt()
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, LINE_SPACING)
                .build()
            drawLines(layout, indent)
            y += PARAGRAPH_GAP
        }

        private fun drawLines(layout: StaticLayout, indent: Float) {
            var line = 0
            while (line < layout.lineCount) {
                val lineHeight = (layout.getLineBottom(line) - layout.getLineTop(line)).toFloat()
                if (y + lineHeight > BOTTOM) newPage()
                drawLine(layout, line, indent)
                y += lineHeight
                line++
            }
        }

        private fun drawLine(layout: StaticLayout, line: Int, indent: Float) {
            val canvas = canvas()
            canvas.save()
            canvas.clipRect(Rect(0, 0, PAGE_WIDTH, PAGE_HEIGHT))
            canvas.translate(MARGIN + indent, y - layout.getLineTop(line))
            canvas.clipRect(0, layout.getLineTop(line), PAGE_WIDTH, layout.getLineBottom(line))
            layout.draw(canvas)
            canvas.restore()
        }

        fun space(height: Float) {
            y += height
        }

        fun drawFooter(footer: DocumentFooter) {
            if (y > BOTTOM - FOOTER_HEIGHT) newPage()
            y = BOTTOM - FOOTER_HEIGHT
            val icon = footer.iconPng?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            val textStart = drawFooterIcon(icon)
            canvas().drawText(footer.sourceLine, textStart, y + FOOTER_LINE, DETAIL)
            canvas().drawText(footer.exportedLine, textStart, y + FOOTER_LINE * 2, DETAIL)
        }

        private fun drawFooterIcon(icon: Bitmap?): Float {
            if (icon == null) return MARGIN
            val target = Rect(MARGIN.toInt(), y.toInt(), (MARGIN + ICON_SIZE).toInt(), (y + ICON_SIZE).toInt())
            canvas().drawBitmap(icon, null, target, null)
            return MARGIN + ICON_SIZE + ICON_GAP
        }

        fun finish() {
            page?.let(pdf::finishPage)
            page = null
        }

        private fun canvas(): Canvas {
            if (page == null) newPage()
            return requireNotNull(page).canvas
        }

        private fun newPage() {
            finish()
            pageNumber++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            y = MARGIN
        }
    }

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 48f
        const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
        const val BOTTOM = PAGE_HEIGHT - MARGIN
        const val LINE_SPACING = 1.25f
        const val PARAGRAPH_GAP = 6f
        const val SECTION_GAP = 14f
        const val LIST_INDENT = 14f
        const val ICON_SIZE = 20f
        const val ICON_GAP = 8f
        const val FOOTER_HEIGHT = 30f
        const val FOOTER_LINE = 11f

        val TITLE = paint(size = 20f, bold = true)
        val DETAIL = paint(size = 9f, color = 0xFF666666.toInt())
        val HEADING = paint(size = 14f, bold = true)
        val SUBHEADING = paint(size = 12f, bold = true)
        val BODY = paint(size = 11f)

        fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK): TextPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }
}
