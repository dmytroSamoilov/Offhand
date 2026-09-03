@file:OptIn(ExperimentalForeignApi::class)

package com.dmytrosamoilov.offhand.core.security

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey

// Applied to directories: backupd skips an excluded directory and everything
// beneath it, and re-applying is idempotent, so calling this at every access
// retrofits installs that predate the exclusion.
fun excludeFromBackup(path: String) {
    NSURL.fileURLWithPath(path).setResourceValue(
        true,
        forKey = NSURLIsExcludedFromBackupKey,
        error = null,
    )
}
