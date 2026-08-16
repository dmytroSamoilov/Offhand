package com.dmytrosamoilov.offhand.feature.notes.presentation

import android.content.Context
import com.dmytrosamoilov.offhand.feature.notes.domain.ShareCacheDirectoryProvider
import java.io.File

class ShareCacheDirectoryProviderImpl(
    private val context: Context,
) : ShareCacheDirectoryProvider {

    override fun shareDirectoryPath(): String = File(context.cacheDir, SHARE_DIR).absolutePath

    private companion object {
        const val SHARE_DIR = "shared_notes"
    }
}
