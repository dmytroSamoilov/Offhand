package com.dmytrosamoilov.offhand.feature.backup.domain

import kotlinx.serialization.Serializable

@Serializable
data class BackupManifest(
    val formatVersion: Int,
    val appVersion: String,
    val platform: String,
    val createdAtEpochMs: Long,
    val noteCount: Int,
    val folderCount: Int,
    val includesAudio: Boolean,
    val totalAudioBytes: Long,
    val styleCount: Int = 0,
)

@Serializable
data class BackupNoteStyle(
    val id: Long,
    val name: String,
    val noteKind: String,
    val language: String,
    val sections: List<BackupNoteStyleSection>,
    val createdAtEpochMs: Long,
)

@Serializable
data class BackupNoteStyleSection(
    val heading: String,
    val guidance: String,
    val format: String,
)

@Serializable
data class BackupFolder(
    val id: Long,
    val name: String,
    val createdAtEpochMs: Long,
)

@Serializable
data class BackupNote(
    val id: Long,
    val title: String,
    val body: String,
    val transcript: String,
    val createdAtEpochMs: Long,
    val transcriptionTimeMs: Long?,
    val structuringTimeMs: Long?,
    val hardwareBackend: String?,
    val audioFileName: String?,
    val durationMs: Long?,
    val status: String,
    val preset: String,
    val folderId: Long?,
    val customStyleId: Long? = null,
)

data class BackupSummary(
    val notes: Int,
    val folders: Int,
    val audioBytes: Long,
)

data class RestoreSummary(
    val notesRestored: Int,
    val notesSkipped: Int,
    val foldersCreated: Int,
    val foldersReused: Int,
)

sealed class BackupException(message: String) : Exception(message) {
    class WrongPassphrase : BackupException("The passphrase does not match this backup")
    class Corrupt(message: String) : BackupException(message)
    class UnsupportedVersion(val version: Int) : BackupException("Unsupported backup format version $version")
}
