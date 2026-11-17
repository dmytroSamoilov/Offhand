package com.dmytrosamoilov.offhand.feature.recording.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RecordWidgetProvider : AppWidgetProvider(), KoinComponent {

    private val updater: RecordWidgetUpdater by inject()
    private val store: RecordWidgetStore by inject()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updater.refresh()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach(store::forget)
    }
}
