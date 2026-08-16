package com.dmytrosamoilov.offhand.feature.notes.presentation

import android.content.Context
import com.dmytrosamoilov.offhand.feature.notes.domain.ShareCacheDirectoryProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareCacheDirectoryProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ShareCacheDirectoryProvider {

    override fun shareDirectoryPath(): String = File(context.cacheDir, SHARE_DIR).absolutePath

    private companion object {
        const val SHARE_DIR = "shared_notes"
    }
}
