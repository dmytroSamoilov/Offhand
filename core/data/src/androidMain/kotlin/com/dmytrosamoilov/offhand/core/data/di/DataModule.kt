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
import com.dmytrosamoilov.offhand.core.data.database.FolderDao
import com.dmytrosamoilov.offhand.core.data.database.NoteDao
import com.dmytrosamoilov.offhand.core.data.database.NoteStyleDao
import com.dmytrosamoilov.offhand.core.data.database.NotesDatabase
import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStylesRepository
import com.dmytrosamoilov.offhand.core.data.domain.EntitlementsRepository
import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.preferences.DataStoreUserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.repository.AlwaysUnlockedEntitlementsRepository
import com.dmytrosamoilov.offhand.core.data.repository.RoomCustomNoteStylesRepository
import com.dmytrosamoilov.offhand.core.data.repository.RoomFoldersRepository
import com.dmytrosamoilov.offhand.core.data.repository.RoomNotesRepository
import com.dmytrosamoilov.offhand.core.security.DatabasePassphraseProvider
import com.dmytrosamoilov.offhand.core.security.PassphraseInvalidatedException
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
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
    singleOf(::RoomNotesRepository) bind NotesRepository::class
    singleOf(::RoomFoldersRepository) bind FoldersRepository::class
    singleOf(::RoomCustomNoteStylesRepository) bind CustomNoteStylesRepository::class
    singleOf(::AlwaysUnlockedEntitlementsRepository) bind EntitlementsRepository::class
    single<UserPreferencesRepository> {
        DataStoreUserPreferencesRepository(createUserPreferencesDataStore(androidContext()), get())
    }
}
