package com.dmytrosamoilov.offhand.feature.notes.domain.export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocxWriterTest {

    private val document = NoteDocument(
        title = "Board <meeting> & more",
        details = listOf(DocumentDetail("Recorded", "Jun 15, 2025 · 15:06")),
        sections = listOf(
            DocumentSection("Overview", listOf(DocumentBlock.Heading("Topics"), DocumentBlock.Bullet("Budget"))),
            DocumentSection("Transcript", listOf(DocumentBlock.Paragraph("We approved the budget."))),
        ),
        footer = DocumentFooter("Created with Offhand", "Exported: today", iconPng = byteArrayOf(1, 2, 3)),
    )

    @Test
    fun `docx is a stored zip with the word package parts and the icon`() {
        val bytes = DocxWriter.write(document)

        val names = zipEntryNames(bytes)
        assertEquals(
            listOf("[Content_Types].xml", "_rels/.rels", "word/_rels/document.xml.rels", "word/styles.xml", "word/document.xml", "word/media/icon.png"),
            names,
        )
        val xml = bytes.decodeToString()
        assertTrue(xml.contains("Board &lt;meeting&gt; &amp; more"))
        assertTrue(xml.contains("• Budget"))
        assertTrue(xml.contains("r:embed=\"rIdIcon\""))
        assertTrue(xml.contains("Created with Offhand"))
    }

    @Test
    fun `without an icon no media part or image relationship is written`() {
        val bytes = DocxWriter.write(document.copy(footer = document.footer.copy(iconPng = null)))

        assertTrue("word/media/icon.png" !in zipEntryNames(bytes))
        assertTrue(!bytes.decodeToString().contains("rIdIcon"))
    }

    @Test
    fun `crc32 matches the reference value for a known string`() {
        assertEquals(0xCBF43926.toInt(), Crc32.of("123456789".encodeToByteArray()))
    }

    private fun zipEntryNames(zip: ByteArray): List<String> {
        val names = mutableListOf<String>()
        var offset = 0
        while (readInt(zip, offset) == LOCAL_HEADER) {
            val nameLength = readShort(zip, offset + 26)
            val extraLength = readShort(zip, offset + 28)
            val size = readInt(zip, offset + 18)
            names += zip.copyOfRange(offset + 30, offset + 30 + nameLength).decodeToString()
            offset += 30 + nameLength + extraLength + size
        }
        assertEquals(CENTRAL_HEADER, readInt(zip, offset))
        assertEndOfDirectory(zip, directoryOffset = offset, entryCount = names.size)
        return names
    }

    private fun assertEndOfDirectory(zip: ByteArray, directoryOffset: Int, entryCount: Int) {
        val end = zip.size - END_RECORD_BYTES
        assertEquals(END_HEADER, readInt(zip, end))
        assertEquals(entryCount, readShort(zip, end + 10))
        assertEquals(end - directoryOffset, readInt(zip, end + 12))
        assertEquals(directoryOffset, readInt(zip, end + 16))
    }

    private fun readShort(bytes: ByteArray, at: Int): Int =
        (bytes[at].toInt() and 0xFF) or ((bytes[at + 1].toInt() and 0xFF) shl 8)

    private fun readInt(bytes: ByteArray, at: Int): Int =
        readShort(bytes, at) or (readShort(bytes, at + 2) shl 16)

    private companion object {
        const val LOCAL_HEADER = 0x04034b50
        const val CENTRAL_HEADER = 0x02014b50
        const val END_HEADER = 0x06054b50
        const val END_RECORD_BYTES = 22
    }
}
