@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.feature.notes.domain.export.AppIconProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.export.NoteDocument
import com.dmytrosamoilov.offhand.feature.notes.domain.export.NotePdfRenderer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

// Swift draws the PDF with UIKit and hands over the app icon; the document
// model crosses as is, only the icon bytes are converted.
interface IosNoteDocumentBridge {

    fun appIconPng(): NSData?

    fun writePdf(document: NoteDocument, iconPng: NSData?, path: String): Boolean
}

class IosNotePdfRenderer(private val bridge: IosNoteDocumentBridge) : NotePdfRenderer {

    override suspend fun render(document: NoteDocument, outputPath: String) = withContext(Dispatchers.IO) {
        val written = bridge.writePdf(document, document.footer.iconPng?.toData(), outputPath)
        check(written) { "PDF rendering failed" }
    }
}

class IosAppIconProvider(private val bridge: IosNoteDocumentBridge) : AppIconProvider {

    private val png: ByteArray? by lazy { bridge.appIconPng()?.toBytes() }

    override fun pngBytes(): ByteArray? = png
}

private fun ByteArray.toData(): NSData =
    usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }

private fun NSData.toBytes(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) {
        result.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
    return result
}
