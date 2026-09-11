package com.dmytrosamoilov.offhand.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_1_2
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_2_3
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_3_4
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_4_5
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_5_6
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_6_7
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_7_8
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_8_9
import com.dmytrosamoilov.offhand.core.data.database.MIGRATION_9_10
import com.dmytrosamoilov.offhand.core.data.billing.ForegroundActivityHolder
import com.dmytrosamoilov.offhand.core.data.billing.PlayProStore
import com.dmytrosamoilov.offhand.core.data.database.FolderDao
import com.dmytrosamoilov.offhand.core.data.database.NoteDao
import com.dmytrosamoilov.offhand.core.data.database.NoteStyleDao
import com.dmytrosamoilov.offhand.core.data.database.NoteSuggestionsDao
import com.dmytrosamoilov.offhand.core.data.database.TranscriptionCheckpointDao
import com.dmytrosamoilov.offhand.core.data.database.NotesDatabase
import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStylesRepository
import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.data.domain.NoteSuggestionsRepository
import com.dmytrosamoilov.offhand.core.data.domain.TranscriptionCheckpointRepository
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusCache
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.preferences.DataStoreProStatusCache
import com.dmytrosamoilov.offhand.core.data.preferences.DataStoreUserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.repository.RoomCustomNoteStylesRepository
import com.dmytrosamoilov.offhand.core.data.repository.RoomFoldersRepository
import com.dmytrosamoilov.offhand.core.data.repository.RoomNoteSuggestionsRepository
import com.dmytrosamoilov.offhand.core.data.repository.RoomTranscriptionCheckpointRepository
import com.dmytrosamoilov.offhand.core.data.repository.DebugOverrideProStore
import com.dmytrosamoilov.offhand.core.data.repository.ProUpgradeCoordinator
import com.dmytrosamoilov.offhand.core.data.repository.RoomNotesRepository
import com.dmytrosamoilov.offhand.core.data.repository.StoreProStatusRepository
import com.dmytrosamoilov.offhand.core.security.DatabasePassphraseProvider
import com.dmytrosamoilov.offhand.core.security.PassphraseInvalidatedException
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import timber.log.Timber

private const val DATABASE_NAME = "offhand-notes.db"
private const val USER_PREFERENCES_FILE_NAME = "user_preferences.preferences_pb"

private fun createNotesDatabase(
    context: Context,
    passphraseProvider: DatabasePassphraseProvider,
): NotesDatabase {
    System.loadLibrary("sqlcipher")
    val passphrase = try {
        passphraseProvider.passphrase()
    } catch (invalidated: PassphraseInvalidatedException) {
        Timber.tag("Security").w(invalidated, "Resetting encrypted storage")
        passphraseProvider.reset()
        context.deleteDatabase(DATABASE_NAME)
        passphraseProvider.passphrase()
    }
    return Room.databaseBuilder(context, NotesDatabase::class.java, DATABASE_NAME)
        .openHelperFactory(SupportOpenHelperFactory(passphrase))
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
        )
        .build()
}

private fun createUserPreferencesDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath {
        context.filesDir.resolve("datastore/$USER_PREFERENCES_FILE_NAME").absolutePath.toPath()
    }

val coreDataModule = module {
    single { createNotesDatabase(androidContext(), get()) }
    factory<NoteDao> { get<NotesDatabase>().noteDao() }
    factory<FolderDao> { get<NotesDatabase>().folderDao() }
    factory<NoteStyleDao> { get<NotesDatabase>().noteStyleDao() }
    factory<NoteSuggestionsDao> { get<NotesDatabase>().noteSuggestionsDao() }
    factory<TranscriptionCheckpointDao> { get<NotesDatabase>().transcriptionCheckpointDao() }
    singleOf(::RoomNotesRepository) bind NotesRepository::class
    singleOf(::RoomFoldersRepository) bind FoldersRepository::class
    singleOf(::RoomCustomNoteStylesRepository) bind CustomNoteStylesRepository::class
    singleOf(::RoomNoteSuggestionsRepository) bind NoteSuggestionsRepository::class
    singleOf(::RoomTranscriptionCheckpointRepository) bind TranscriptionCheckpointRepository::class
    single { ForegroundActivityHolder() }
    single<ProStore>(named(PLATFORM_PRO_STORE)) { PlayProStore(androidContext(), get()) }
    single<ProStore> { DebugOverrideProStore(get(named(PLATFORM_PRO_STORE)), get()) }
    single<ProStatusCache> { DataStoreProStatusCache(get()) }
    singleOf(::StoreProStatusRepository) bind ProStatusRepository::class
    singleOf(::ProUpgradeCoordinator) bind ProUpgradeGate::class
    single<DataStore<Preferences>> { createUserPreferencesDataStore(androidContext()) }
    single<UserPreferencesRepository> { DataStoreUserPreferencesRepository(get(), get()) }
}
