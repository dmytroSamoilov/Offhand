package com.dmytrosamoilov.offhand.feature.recording.widget

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.dmytrosamoilov.offhand.core.data.domain.AppForegroundState
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.feature.recording.R
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.domain.SessionPhase
import com.dmytrosamoilov.offhand.feature.recording.service.RecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Draws every record widget and decides what a tap does. A tap records
// straight away, without opening the app, only when that can work: the plan
// is Pro and the microphone is already allowed. Otherwise it opens the app,
// which asks for what is missing and then starts the recording itself.
class RecordWidgetUpdater(
    private val context: Context,
    private val sessionManager: RecordingSessionManager,
    private val proStatusRepository: ProStatusRepository,
    private val appForegroundState: AppForegroundState,
    private val store: RecordWidgetStore,
) {

    // Application-lifetime scope: lives as long as the process, never cancelled.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var isPro = false

    // The foreground is watched because that is where the microphone
    // permission changes: coming back from the system dialog redraws the tap.
    fun start() {
        scope.launch {
            combine(
                sessionManager.session.map { it.phase == SessionPhase.RECORDING }.distinctUntilChanged(),
                proStatusRepository.observeStatus().map { it.isPro }.distinctUntilChanged(),
                appForegroundState.isInForeground,
            ) { isRecording, pro, _ ->
                isPro = pro
                isRecording
            }.collect { isRecording -> render(isRecording) }
        }
    }

    fun refresh() {
        render(sessionManager.session.value.phase == SessionPhase.RECORDING)
    }

    private fun render(isRecording: Boolean) {
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = manager.getAppWidgetIds(ComponentName(context, RecordWidgetProvider::class.java))
        widgetIds.forEach { widgetId -> manager.updateAppWidget(widgetId, views(widgetId, isRecording)) }
    }

    private fun views(widgetId: Int, isRecording: Boolean): RemoteViews {
        val folder = store.folderOf(widgetId)
        return RemoteViews(context.packageName, R.layout.record_widget).apply {
            setInt(
                R.id.record_widget_root,
                "setBackgroundResource",
                if (isRecording) R.drawable.record_widget_background_recording else R.drawable.record_widget_background,
            )
            setImageViewResource(
                R.id.record_widget_icon,
                if (isRecording) R.drawable.ic_record_widget_stop else R.drawable.ic_record_widget_mic,
            )
            setTextViewText(
                R.id.record_widget_title,
                context.getString(if (isRecording) R.string.record_widget_stop else R.string.record_widget_record),
            )
            setTextViewText(R.id.record_widget_subtitle, folder?.name ?: context.getString(R.string.record_widget_no_folder))
            setOnClickPendingIntent(R.id.record_widget_root, tapIntent(widgetId, folder?.id, isRecording))
        }
    }

    private fun tapIntent(widgetId: Int, folderId: Long?, isRecording: Boolean): PendingIntent? = when {
        isRecording -> PendingIntent.getService(context, STOP_REQUEST, RecordingService.stopIntent(context), FLAGS)
        canRecordWithoutTheApp() ->
            PendingIntent.getForegroundService(context, widgetId, RecordingService.startIntent(context, folderId), FLAGS)
        else -> openAppIntent(widgetId, folderId)
    }

    private fun canRecordWithoutTheApp(): Boolean =
        isPro && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun openAppIntent(widgetId: Int, folderId: Long?): PendingIntent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launchIntent.putExtra(RecordingService.EXTRA_RECORD_REQUEST, true)
        folderId?.let { launchIntent.putExtra(RecordingService.EXTRA_RECORD_FOLDER_ID, it) }
        return PendingIntent.getActivity(context, widgetId, launchIntent, FLAGS)
    }

    private companion object {
        const val STOP_REQUEST = -1
        const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    }
}
