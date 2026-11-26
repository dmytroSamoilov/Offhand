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
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_6_7
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_7_8
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_8_9
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_10_11
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_11_12
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_12_13
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_9_10
import com.dmytrosamoilov.offhand.core.data.database.FolderDao
import com.dmytrosamoilov.offhand.core.data.database.NoteDao
import com.dmytrosamoilov.offhand.core.data.database.NoteStyleDao
import com.dmytrosamoilov.offhand.core.data.database.NoteSuggestionsDao
import com.dmytrosamoilov.offhand.core.data.database.NoteWaveformDao
import com.dmytrosamoilov.offhand.core.data.database.TranscriptionCheckpointDao
import com.dmytrosamoilov.offhand.core.data.database.NotesDatabase
import com.dmytrosamoilov.offhand.core.data.database.applyCompleteUnlessOpenProtection
import com.dmytrosamoilov.offhand.core.data.database.createProtectedDatabaseDirectory
import com.dmytrosamoilov.offhand.core.data.database.iosDocumentsDirectory
import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStylesRepository
import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.data.domain.NoteSuggestionsRepository
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.core.data.domain.NoteWaveformRepository
import com.dmytrosamoilov.offhand.core.data.domain.TranscriptionCheckpointRepository
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusCache
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.common.ModelDownloadPolicy
import com.dmytrosamoilov.offhand.core.data.domain.ModelDownloadLauncher
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.preferences.DataStoreProStatusCache
import com.dmytrosamoilov.offhand.core.data.preferences.DataStoreUserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.repository.RoomCustomNoteStylesRepository
import com.dmytrosamoilov.offhand.core.data.repository.RoomFoldersRepository
import com.dmytrosamoilov.offhand.core.data.repository.RoomNoteSuggestionsRepository
import com.dmytrosamoilov.offhand.core.data.repository.ConsentGatedAnalyticsTracker
import com.dmytrosamoilov.offhand.core.data.repository.RoomNoteWaveformRepository
import com.dmytrosamoilov.offhand.core.data.repository.RoomTranscriptionCheckpointRepository
import com.dmytrosamoilov.offhand.core.data.repository.DebugOverrideProStore
import com.dmytrosamoilov.offhand.core.data.repository.ProUpgradeCoordinator
import com.dmytrosamoilov.offhand.core.data.repository.RoomNotesRepository
import com.dmytrosamoilov.offhand.core.data.repository.StoreProStatusRepository
import com.dmytrosamoilov.offhand.core.data.repository.ICloudProTrialRecordStore
import com.dmytrosamoilov.offhand.core.data.repository.StoredProTrialRepository
import com.dmytrosamoilov.offhand.core.data.repository.DataStoreProTrialUsageRepository
import com.dmytrosamoilov.offhand.core.data.repository.DataStoreSurveyRepository
import com.dmytrosamoilov.offhand.core.data.repository.TrialUsageCountingTracker
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsageRepository
import com.dmytrosamoilov.offhand.core.data.domain.SurveyRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRecordStore
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRepository
import com.dmytrosamoilov.offhand.core.security.excludeFromBackup
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.Path.Companion.toPath
import platform.Foundation.NSFileManager
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
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
    factory<FolderDao> { get<NotesDatabase>().folderDao() }
    factory<NoteStyleDao> { get<NotesDatabase>().noteStyleDao() }
    factory<NoteSuggestionsDao> { get<NotesDatabase>().noteSuggestionsDao() }
    factory<TranscriptionCheckpointDao> { get<NotesDatabase>().transcriptionCheckpointDao() }
    factory<NoteWaveformDao> { get<NotesDatabase>().noteWaveformDao() }
    singleOf(::RoomNotesRepository) bind NotesRepository::class
    singleOf(::RoomFoldersRepository) bind FoldersRepository::class
    singleOf(::RoomCustomNoteStylesRepository) bind CustomNoteStylesRepository::class
    singleOf(::RoomNoteSuggestionsRepository) bind NoteSuggestionsRepository::class
    singleOf(::RoomTranscriptionCheckpointRepository) bind TranscriptionCheckpointRepository::class
    singleOf(::RoomNoteWaveformRepository) bind NoteWaveformRepository::class
    single<ProStore> { DebugOverrideProStore(get(named(PLATFORM_PRO_STORE)), get()) }
    single<ProStatusCache> { DataStoreProStatusCache(get()) }
    single<ProTrialRecordStore> { ICloudProTrialRecordStore() }
    single<ProTrialRepository> { StoredProTrialRepository(get(), get()) }
    singleOf(::StoreProStatusRepository) bind ProStatusRepository::class
    singleOf(::ProUpgradeCoordinator) bind ProUpgradeGate::class
    singleOf(::ModelDownloadPolicy)
    singleOf(::ModelDownloadLauncher)
    single<ProTrialUsageRepository> { DataStoreProTrialUsageRepository(get()) }
    single<SurveyRepository> { DataStoreSurveyRepository(get()) }
    single<AnalyticsTracker> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        TrialUsageCountingTracker(ConsentGatedAnalyticsTracker(get(), get(), get(), scope), get(), get(), scope)
    }
    single<DataStore<Preferences>> { createUserPreferencesDataStore() }
    single<UserPreferencesRepository> { DataStoreUserPreferencesRepository(get(), get(), get()) }
}
