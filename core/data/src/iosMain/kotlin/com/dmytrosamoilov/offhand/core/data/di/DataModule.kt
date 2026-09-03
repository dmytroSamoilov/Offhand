@file:OptIn(ExperimentalForeignApi::class)

package com.dmytrosamoilov.offhand.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_1_2
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_2_3
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_3_4
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_4_5
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_5_6
import com.dmytrosamoilov.offhand.core.data.database.NoteDao
import com.dmytrosamoilov.offhand.core.data.database.NotesDatabase
import com.dmytrosamoilov.offhand.core.data.database.applyCompleteUnlessOpenProtection
import com.dmytrosamoilov.offhand.core.data.database.createProtectedDatabaseDirectory
import com.dmytrosamoilov.offhand.core.data.database.iosDocumentsDirectory
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.preferences.DataStoreUserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.repository.RoomNotesRepository
import com.dmytrosamoilov.offhand.core.security.excludeFromBackup
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.Path.Companion.toPath
import platform.Foundation.NSFileManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

private const val DATABASE_NAME = "offhand-notes.db"
private const val USER_PREFERENCES_FILE_NAME = "user_preferences.preferences_pb"

private fun createNotesDatabase(): NotesDatabase {
    val databasePath = "${createProtectedDatabaseDirectory()}/$DATABASE_NAME"
    val database = Room.databaseBuilder<NotesDatabase>(databasePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
        )
        .build()
    applyCompleteUnlessOpenProtection(databasePath)
    return database
}

private fun createUserPreferencesDataStore(): DataStore<Preferences> {
    val directory = "${iosDocumentsDirectory()}/datastore"
    NSFileManager.defaultManager.createDirectoryAtPath(
        directory,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    excludeFromBackup(directory)
    return PreferenceDataStoreFactory.createWithPath {
        "$directory/$USER_PREFERENCES_FILE_NAME".toPath()
    }
}

val coreDataModule = module {
    single { createNotesDatabase() }
    factory<NoteDao> { get<NotesDatabase>().noteDao() }
    singleOf(::RoomNotesRepository) bind NotesRepository::class
    single<UserPreferencesRepository> {
        DataStoreUserPreferencesRepository(createUserPreferencesDataStore(), get())
    }
}
