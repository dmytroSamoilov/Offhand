package com.dmytrosamoilov.offhand.feature.recording.widget

import android.content.Context

// Which folder each widget records into. The name is kept next to the id so
// a widget can be drawn before the app is unlocked and its database opened.
class RecordWidgetStore(context: Context) {

    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun folderOf(widgetId: Int): WidgetFolder? {
        val id = preferences.getLong(idKey(widgetId), NO_FOLDER)
        val name = preferences.getString(nameKey(widgetId), null)
        return if (id == NO_FOLDER || name == null) null else WidgetFolder(id, name)
    }

    fun setFolder(widgetId: Int, folder: WidgetFolder?) {
        preferences.edit().apply {
            if (folder == null) {
                remove(idKey(widgetId)).remove(nameKey(widgetId))
            } else {
                putLong(idKey(widgetId), folder.id).putString(nameKey(widgetId), folder.name)
            }
        }.apply()
    }

    fun forget(widgetId: Int) = setFolder(widgetId, null)

    private fun idKey(widgetId: Int) = "folder_id_$widgetId"

    private fun nameKey(widgetId: Int) = "folder_name_$widgetId"

    private companion object {
        const val FILE_NAME = "record_widget"
        const val NO_FOLDER = -1L
    }
}

data class WidgetFolder(val id: Long, val name: String)
