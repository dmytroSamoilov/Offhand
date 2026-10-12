@file:OptIn(ExperimentalForeignApi::class)

package com.dmytrosamoilov.offhand.feature.notes.presentation

import com.dmytrosamoilov.offhand.feature.notes.domain.ShareCacheDirectoryProvider
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class IosShareCacheDirectoryProvider : ShareCacheDirectoryProvider {

    override fun shareDirectoryPath(): String {
        val caches = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            .first() as String
        return "$caches/shared_notes"
    }

    override suspend fun clearShareDirectory() {
        withContext(Dispatchers.IO) {
            NSFileManager.defaultManager.removeItemAtPath(shareDirectoryPath(), error = null)
        }
    }
}

object NoOpInAppReviewLauncher : InAppReviewLauncher
