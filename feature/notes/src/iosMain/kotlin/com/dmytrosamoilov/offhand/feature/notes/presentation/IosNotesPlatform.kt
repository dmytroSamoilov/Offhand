@file:OptIn(ExperimentalForeignApi::class)

package com.dmytrosamoilov.offhand.feature.notes.presentation

import com.dmytrosamoilov.offhand.feature.notes.domain.ShareCacheDirectoryProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.review.AppInstallInfoProvider
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileCreationDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970

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

class IosAppInstallInfoProvider : AppInstallInfoProvider {

    override val installedAtMs: Long
        get() {
            val documents =
                NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
                    .first() as String
            val attributes = NSFileManager.defaultManager
                .attributesOfItemAtPath(documents, error = null)
            val createdAt = attributes?.get(NSFileCreationDate) as? NSDate
            return ((createdAt?.timeIntervalSince1970 ?: 0.0) * MS_PER_SECOND).toLong()
        }

    private companion object {
        const val MS_PER_SECOND = 1000.0
    }
}

object NoOpInAppReviewLauncher : InAppReviewLauncher
