package com.dmytrosamoilov.offhand.feature.backup.domain

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source

class PathBackupFile(private val path: String) : BackupFile {

    override fun openWrite(): Sink = FileSystem.SYSTEM.sink(path.toPath())

    override fun openRead(): Source = FileSystem.SYSTEM.source(path.toPath())
}
