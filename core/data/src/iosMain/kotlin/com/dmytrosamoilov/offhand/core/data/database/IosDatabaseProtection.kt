@file:OptIn(ExperimentalForeignApi::class)

package com.dmytrosamoilov.offhand.core.data.database

import com.dmytrosamoilov.offhand.core.security.excludeFromBackup
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileProtectionCompleteUnlessOpen
import platform.Foundation.NSFileProtectionKey
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

internal fun iosDocumentsDirectory(): String =
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String

// SQLite creates -wal/-shm siblings after the builder runs; new files inherit the parent
// directory's protection class, so the database lives in its own protected directory.
internal fun createProtectedDatabaseDirectory(): String {
    val directory = "${iosDocumentsDirectory()}/database"
    if (!NSFileManager.defaultManager.fileExistsAtPath(directory)) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            directory,
            withIntermediateDirectories = true,
            attributes = mapOf(NSFileProtectionKey to NSFileProtectionCompleteUnlessOpen),
            error = null,
        )
    }
    excludeFromBackup(directory)
    return directory
}

internal fun applyCompleteUnlessOpenProtection(filePath: String) {
    NSFileManager.defaultManager.setAttributes(
        mapOf(NSFileProtectionKey to NSFileProtectionCompleteUnlessOpen),
        ofItemAtPath = filePath,
        error = null,
    )
}
