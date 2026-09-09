package com.dmytrosamoilov.offhand.feature.backup.domain.usecase

import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.core.data.domain.Folder
import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.security.AndroidBackupCrypto
import com.dmytrosamoilov.offhand.core.security.AudioInputStream
import com.dmytrosamoilov.offhand.core.security.AudioOutputStream
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupException
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupFile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.Sink
import okio.Source
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupRoundTripTest {

    private val crypto = AndroidBackupCrypto()
    private val buildInfo = BuildInfo(isDebugBuild = true, appVersion = "1.3.0", platform = "android")
    private val passphrase = "correct horse battery".encodeToByteArray()

    private val sourceAudio = mapOf("note-a.pcm.enc" to ByteArray(70_000) { (it % 251).toByte() })
    private val sourceFolders = listOf(Folder(id = 10, name = "Work", createdAtEpochMs = 1))
    private val sourceNotes = listOf(
        note(id = 1, title = "Budget", createdAt = 100, audio = "note-a.pcm.enc", folderId = 10),
        note(id = 2, title = "Draft", createdAt = 200, audio = null, folderId = null, status = NoteStatus.PROCESSING),
        note(id = 3, title = "Standup", createdAt = 300, audio = null, folderId = null),
    )

    @Test
    fun `backup and restore reproduce notes, folders and audio on a fresh device`() = runTest {
        val archive = Buffer()
        val summary = createBackup().invoke(bufferFile(archive), passphrase, includeAudio = true)

        assertEquals(2, summary.notes)
        assertEquals(70_000L, summary.audioBytes)

        val target = Target()
        val restored = RestoreBackupUseCase(target.notes, target.folders, target.audioStore, crypto)
            .invoke(bufferFile(archive), passphrase)

        assertEquals(2, restored.notesRestored)
        assertEquals(1, restored.foldersCreated)
        val budget = target.saved.values.first { it.title == "Budget" }
        assertEquals(target.createdFolderIds.single(), budget.folderId)
        assertArrayEquals(sourceAudio.getValue("note-a.pcm.enc"), target.audioFiles.getValue(budget.audioFileName!!))
        assertNull(target.saved.values.first { it.title == "Standup" }.audioFileName)
    }

    @Test
    fun `restore merges into existing data and skips duplicates`() = runTest {
        val archive = Buffer()
        createBackup().invoke(bufferFile(archive), passphrase, includeAudio = false)

        val target = Target(
            existingFolders = listOf(Folder(id = 77, name = "work", createdAtEpochMs = 5)),
            existingNotes = listOf(note(id = 50, title = "Budget", createdAt = 100, audio = null, folderId = null)),
        )
        val restored = RestoreBackupUseCase(target.notes, target.folders, target.audioStore, crypto)
            .invoke(bufferFile(archive), passphrase)

        assertEquals(1, restored.notesRestored)
        assertEquals(1, restored.notesSkipped)
        assertEquals(1, restored.foldersReused)
        assertEquals(0, restored.foldersCreated)
    }

    @Test
    fun `wrong passphrase fails before touching the database`() = runTest {
        val archive = Buffer()
        createBackup().invoke(bufferFile(archive), passphrase, includeAudio = false)
        val target = Target()

        assertThrows(BackupException.WrongPassphrase::class.java) {
            runBlocking {
                RestoreBackupUseCase(target.notes, target.folders, target.audioStore, crypto)
                    .invoke(bufferFile(archive), "nope".encodeToByteArray())
            }
        }
        assertEquals(0, target.saved.size)
    }

    private fun createBackup() = CreateBackupUseCase(
        notesRepository = mockk { every { observeNotes() } returns MutableStateFlow(sourceNotes) },
        foldersRepository = mockk { every { observeFolders() } returns MutableStateFlow(sourceFolders) },
        audioStore = mockk {
            every { pcmSizeOf(any()) } answers { sourceAudio.getValue(firstArg()).size.toLong() }
            every { openForRead(any()) } answers { AudioInputStream(ByteArrayInputStream(sourceAudio.getValue(firstArg()))) }
        },
        crypto = crypto,
        buildInfo = buildInfo,
    )

    private fun bufferFile(buffer: Buffer): BackupFile = object : BackupFile {
        override fun openWrite(): Sink = buffer
        override fun openRead(): Source = Buffer().write(buffer.copy().readByteArray())
    }

    private class Target(
        existingFolders: List<Folder> = emptyList(),
        existingNotes: List<Note> = emptyList(),
    ) {
        val saved = linkedMapOf<Long, Note>().apply { existingNotes.forEach { put(it.id, it) } }
        val createdFolderIds = mutableListOf<Long>()
        val audioFiles = mutableMapOf<String, ByteArray>()
        private val folderList = existingFolders.toMutableList()
        private var nextId = 1000L

        val notes: NotesRepository = mockk {
            every { observeNotes() } answers { MutableStateFlow(saved.values.toList()) }
            coEvery { createNote(any()) } answers {
                val id = nextId++
                saved[id] = firstArg<Note>().copy(id = id)
                id
            }
            coEvery { getNote(any()) } answers { saved[firstArg()] }
            coEvery { updateNote(any()) } answers { saved[firstArg<Note>().id] = firstArg() }
        }

        val folders: FoldersRepository = mockk {
            every { observeFolders() } answers { MutableStateFlow(folderList.toList()) }
            coEvery { createFolder(any()) } answers {
                val id = nextId++
                folderList += Folder(id, firstArg(), 0)
                createdFolderIds += id
                id
            }
        }

        val audioStore: EncryptedAudioStore = mockk {
            every { newRecordingFileName() } answers { "restored-${nextId++}.pcm.enc" }
            every { openForWrite(any()) } answers { AudioOutputStream(CapturingStream(firstArg(), audioFiles)) }
        }
    }

    private class CapturingStream(
        private val name: String,
        private val files: MutableMap<String, ByteArray>,
    ) : OutputStream() {
        private val bytes = ByteArrayOutputStream()

        override fun write(b: Int) = bytes.write(b)

        override fun write(b: ByteArray, off: Int, len: Int) = bytes.write(b, off, len)

        override fun close() {
            files[name] = bytes.toByteArray()
        }
    }

    private fun note(
        id: Long,
        title: String,
        createdAt: Long,
        audio: String?,
        folderId: Long?,
        status: NoteStatus = NoteStatus.READY,
    ) = Note(
        id = id,
        title = title,
        body = "## Summary\n- $title",
        transcript = "$title transcript",
        createdAtEpochMs = createdAt,
        transcriptionTimeMs = 10,
        structuringTimeMs = 20,
        hardwareBackend = "CPU",
        audioFileName = audio,
        durationMs = 5_000,
        status = status,
        folderId = folderId,
    )
}
