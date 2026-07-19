package com.dmytrosamoilov.offhand.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [NoteEntity::class], version = 5, exportSchema = false)
internal abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
