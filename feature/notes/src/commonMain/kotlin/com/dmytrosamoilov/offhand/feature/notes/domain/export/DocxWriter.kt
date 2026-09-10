package com.dmytrosamoilov.offhand.feature.notes.domain.export

object DocxWriter {

    fun write(document: NoteDocument): ByteArray {
        val zip = ZipWriter()
        val hasIcon = document.footer.iconPng != null
        zip.add("[Content_Types].xml", contentTypes(hasIcon).encodeToByteArray())
        zip.add("_rels/.rels", ROOT_RELS.encodeToByteArray())
        zip.add("word/_rels/document.xml.rels", documentRels(hasIcon).encodeToByteArray())
        zip.add("word/styles.xml", STYLES.encodeToByteArray())
        zip.add("word/document.xml", body(document).encodeToByteArray())
        document.footer.iconPng?.let { zip.add("word/media/icon.png", it) }
        return zip.toByteArray()
    }

    private fun body(document: NoteDocument): String = buildString {
        append(DOCUMENT_HEAD)
        append(paragraph(document.title, style = "Title"))
        document.details.forEach { append(paragraph("${it.label}: ${it.value}", style = "Detail")) }
        document.sections.forEach { section ->
            append(paragraph(section.heading, style = "Heading1"))
            section.blocks.forEach { append(block(it)) }
        }
        append(footer(document.footer))
        append(DOCUMENT_TAIL)
    }

    private fun block(block: DocumentBlock): String = when (block) {
        is DocumentBlock.Heading -> paragraph(block.text, style = "Heading2")
        is DocumentBlock.Paragraph -> paragraph(block.text)
        is DocumentBlock.Bullet -> paragraph("• ${block.text}", style = "ListItem")
        is DocumentBlock.Numbered -> paragraph("${block.number}. ${block.text}", style = "ListItem")
    }

    private fun footer(footer: DocumentFooter): String = buildString {
        append(paragraph("", style = "Detail"))
        append("<w:p><w:pPr><w:pStyle w:val=\"Detail\"/></w:pPr>")
        if (footer.iconPng != null) append(ICON_RUN)
        append("<w:r><w:t xml:space=\"preserve\">${escape(footer.sourceLine)}</w:t></w:r></w:p>")
        append(paragraph(footer.exportedLine, style = "Detail"))
    }

    private fun paragraph(text: String, style: String? = null): String {
        val properties = style?.let { "<w:pPr><w:pStyle w:val=\"$it\"/></w:pPr>" }.orEmpty()
        return "<w:p>$properties<w:r><w:t xml:space=\"preserve\">${escape(text)}</w:t></w:r></w:p>"
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .filter { it >= ' ' || it == '\t' }

    private fun contentTypes(hasIcon: Boolean): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
${if (hasIcon) "<Default Extension=\"png\" ContentType=\"image/png\"/>" else ""}
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""

    private fun documentRels(hasIcon: Boolean): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
${if (hasIcon) "<Relationship Id=\"rIdIcon\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/icon.png\"/>" else ""}
</Relationships>"""

    private const val ROOT_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    private const val DOCUMENT_HEAD = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><w:body>"""

    private const val DOCUMENT_TAIL = """<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134" w:header="708" w:footer="708" w:gutter="0"/></w:sectPr></w:body></w:document>"""

    private const val ICON_EMU = 228600

    private const val ICON_RUN = """<w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="76200"><wp:extent cx="$ICON_EMU" cy="$ICON_EMU"/><wp:docPr id="1" name="Offhand"/><a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic><pic:nvPicPr><pic:cNvPr id="1" name="icon.png"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed="rIdIcon"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="$ICON_EMU" cy="$ICON_EMU"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r>"""

    private const val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Calibri"/><w:sz w:val="22"/></w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after="120" w:line="276" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>
<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style>
<w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="60"/></w:pPr><w:rPr><w:b/><w:sz w:val="40"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Detail"><w:name w:val="Detail"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="0"/></w:pPr><w:rPr><w:color w:val="666666"/><w:sz w:val="18"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:pPr><w:keepNext/><w:spacing w:before="360" w:after="120"/></w:pPr><w:rPr><w:b/><w:sz w:val="28"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:pPr><w:keepNext/><w:spacing w:before="240" w:after="80"/></w:pPr><w:rPr><w:b/><w:sz w:val="24"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="ListItem"><w:name w:val="List Item"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:after="60"/><w:ind w:left="360" w:hanging="360"/></w:pPr></w:style>
</w:styles>"""
}
