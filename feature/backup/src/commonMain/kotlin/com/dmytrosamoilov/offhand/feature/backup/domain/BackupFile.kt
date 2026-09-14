package com.dmytrosamoilov.offhand.feature.backup.domain

import okio.Sink
import okio.Source

interface BackupFile {

    fun openWrite(): Sink

    fun openRead(): Source
}
