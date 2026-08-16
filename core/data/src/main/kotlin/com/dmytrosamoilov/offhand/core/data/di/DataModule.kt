package com.dmytrosamoilov.offhand.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dmytrosamoilov.offhand.core.data.database.NoteDao
import com.dmytrosamoilov.offhand.core.data.database.NotesDatabase
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.preferences.DataStoreUserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.repository.RoomNotesRepository
import com.dmytrosamoilov.offhand.core.security.DatabasePassphraseProvider
import com.dmytrosamoilov.offhand.core.security.PassphraseInvalidatedException
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import timber.log.Timber

private const val DATABASE_NAME = "offhand-notes.db"

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN transcript TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN audioFileName TEXT")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN status TEXT NOT NULL DEFAULT 'READY'")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN durationMs INTEGER")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN preset TEXT NOT NULL DEFAULT 'SUMMARY'")
    }
}

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
        )
        .build()
}

val coreDataModule = module {
    single { createNotesDatabase(androidContext(), get()) }
    factory<NoteDao> { get<NotesDatabase>().noteDao() }
    singleOf(::RoomNotesRepository) bind NotesRepository::class
    singleOf(::DataStoreUserPreferencesRepository) bind UserPreferencesRepository::class
}
