package com.dmytrosamoilov.offhand.feature.backup.domain

import android.content.ContentResolver
import android.net.Uri
import okio.Sink
import okio.Source
import okio.sink
import okio.source

class UriBackupFile(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
) : BackupFile {

    override fun openWrite(): Sink =
        requireNotNull(contentResolver.openOutputStream(uri, "wt")) { "Cannot write to the chosen location" }.sink()

    override fun openRead(): Source =
        requireNotNull(contentResolver.openInputStream(uri)) { "Cannot read the chosen file" }.source()
}
