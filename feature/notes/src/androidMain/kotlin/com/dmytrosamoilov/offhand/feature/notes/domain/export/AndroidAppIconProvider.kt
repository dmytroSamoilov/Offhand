package com.dmytrosamoilov.offhand.feature.notes.domain.export

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import java.io.ByteArrayOutputStream

class AndroidAppIconProvider(
    private val context: Context,
) : AppIconProvider {

    private val png: ByteArray? by lazy { renderIcon() }

    override fun pngBytes(): ByteArray? = png

    private fun renderIcon(): ByteArray? = runCatching {
        val bitmap = context.packageManager.getApplicationIcon(context.applicationInfo).toBitmap(ICON_PX, ICON_PX)
        ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)
            output.toByteArray()
        }
    }.getOrNull()

    private companion object {
        const val ICON_PX = 128
        const val PNG_QUALITY = 100
    }
}
