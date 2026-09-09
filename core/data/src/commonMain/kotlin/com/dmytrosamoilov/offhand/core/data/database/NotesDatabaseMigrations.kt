package com.dmytrosamoilov.offhand.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE notes ADD COLUMN transcript TEXT NOT NULL DEFAULT ''")
    }
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE notes ADD COLUMN audioFileName TEXT")
    }
}

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE notes ADD COLUMN status TEXT NOT NULL DEFAULT 'READY'")
    }
}

internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE notes ADD COLUMN durationMs INTEGER")
    }
}

internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE notes ADD COLUMN preset TEXT NOT NULL DEFAULT 'SUMMARY'")
    }
}

internal val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS folders (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "createdAtEpochMs INTEGER NOT NULL)",
        )
        connection.execSQL("ALTER TABLE notes ADD COLUMN folderId INTEGER")
    }
}
