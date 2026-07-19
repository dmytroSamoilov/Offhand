package com.dmytrosamoilov.offhand.feature.recording.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.dmytrosamoilov.offhand.core.designsystem.R as DesignR
import com.dmytrosamoilov.offhand.feature.recording.R
import com.dmytrosamoilov.offhand.feature.recording.domain.NoteProcessingEvent
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.domain.SessionPhase
import com.dmytrosamoilov.offhand.feature.recording.presentation.formatElapsed
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject
    lateinit var sessionManager: RecordingSessionManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(
                    recordingNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
                )
                sessionManager.start()
                observeSession()
            }
            ACTION_PAUSE -> sessionManager.pause()
            ACTION_RESUME -> sessionManager.resume()
            ACTION_STOP -> sessionManager.stop()
            ACTION_RETRY_NOTE -> startNoteRetry(intent)
        }
        return START_NOT_STICKY
    }

    private fun startNoteRetry(intent: Intent) {
        val noteId = intent.getLongExtra(EXTRA_RETRY_NOTE_ID, -1L)
        val audioFileName = intent.getStringExtra(EXTRA_RETRY_AUDIO_FILE)
        if (noteId <= 0 || audioFileName.isNullOrBlank()) return
        startForeground(processingNotification(), processingForegroundType())
        sessionManager.retryNote(noteId, audioFileName)
        observeSession()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeSession() {
        if (observeJob != null) return
        sessionManager.events
            .onEach { event -> notifyProcessingFinished(event) }
            .launchIn(serviceScope)
        observeJob = combine(
            sessionManager.session,
            sessionManager.processingNoteIds,
            sessionManager.noteProgress,
        ) { session, processingIds, progress ->
            ServiceState(
                phase = session.phase,
                isPaused = session.isPaused,
                isProcessing = processingIds.isNotEmpty(),
                processingPercent = progress.values.minOrNull(),
            )
        }
            .distinctUntilChanged()
            .onEach { state -> applyServiceState(state) }
            .launchIn(serviceScope)
    }

    private fun applyServiceState(state: ServiceState) {
        when {
            state.phase == SessionPhase.RECORDING ->
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, recordingNotification(state.isPaused))
            state.phase == SessionPhase.DRAINING || state.isProcessing -> startForeground(
                processingNotification(state.processingPercent),
                processingForegroundType(),
            )
            else -> {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun notifyProcessingFinished(event: NoteProcessingEvent) {
        val notification = when (event) {
            is NoteProcessingEvent.Completed -> noteFinishedNotification(
                noteId = event.noteId,
                title = getString(R.string.recording_notification_ready_title),
                text = getString(R.string.recording_notification_ready_text),
            )
            is NoteProcessingEvent.Failed -> noteFinishedNotification(
                noteId = event.noteId,
                title = getString(R.string.recording_notification_note_failed_title),
                text = getString(R.string.recording_notification_note_failed_text),
            )
        }
        getSystemService(NotificationManager::class.java)
            .notify(noteNotificationId(event.noteId), notification)
    }

    private fun noteFinishedNotification(noteId: Long, title: String, text: String): Notification =
        NotificationCompat.Builder(this, NOTES_CHANNEL_ID)
            .setSmallIcon(DesignR.drawable.ic_app_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openNoteIntent(noteId))
            .build()

    private fun startForeground(notification: Notification, type: Int) {
        // ServiceCompat masks the type against a compile-time list of known
        // types and strips FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING (API 35+),
        // which crashes with "Starting FGS with type none" on targetSdk 35+.
        startForeground(NOTIFICATION_ID, notification, type)
    }

    private fun processingForegroundType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        }

    private fun recordingNotification(isPaused: Boolean = false): Notification =
        if (isPaused) pausedNotification() else activeRecordingNotification()

    private fun activeRecordingNotification(): Notification = liveRecordingBuilder()
        .setContentTitle(getString(R.string.recording_notification_recording_title))
        .setContentText(getString(R.string.recording_notification_recording_text))
        .setUsesChronometer(true)
        .setWhen(System.currentTimeMillis() - sessionManager.vad.value.totalElapsedMs)
        .setStyle(NotificationCompat.ProgressStyle().setProgressIndeterminate(true))
        .addAction(pauseOrResumeAction(isPaused = false))
        .addAction(stopAction())
        .build()

    private fun pausedNotification(): Notification = liveRecordingBuilder()
        .setContentTitle(getString(R.string.recording_notification_paused_title))
        .setContentText(
            getString(
                R.string.recording_notification_paused_text,
                formatElapsed(sessionManager.vad.value.totalElapsedMs),
            ),
        )
        .addAction(pauseOrResumeAction(isPaused = true))
        .addAction(stopAction())
        .build()

    private fun liveRecordingBuilder(): NotificationCompat.Builder = baseNotification()
        .setRequestPromotedOngoing(true)
        .setShortCriticalText(getString(R.string.recording_notification_chip))

    private fun pauseOrResumeAction(isPaused: Boolean): NotificationCompat.Action = if (isPaused) {
        notificationAction(R.string.recording_notification_action_resume, ACTION_RESUME)
    } else {
        notificationAction(R.string.recording_notification_action_pause, ACTION_PAUSE)
    }

    private fun stopAction(): NotificationCompat.Action =
        notificationAction(R.string.recording_notification_action_stop, ACTION_STOP)

    private fun notificationAction(titleRes: Int, action: String): NotificationCompat.Action =
        NotificationCompat.Action(
            0,
            getString(titleRes),
            PendingIntent.getService(
                this,
                action.hashCode(),
                serviceIntent(this, action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )

    private fun processingNotification(percent: Int? = null): Notification {
        val builder = baseNotification()
            .setContentTitle(getString(R.string.recording_notification_processing_title))
            .setContentText(getString(R.string.recording_notification_processing_text))
        if (percent != null) {
            builder.setProgress(PROGRESS_MAX, percent, false)
        }
        sessionManager.processingNoteIds.value.firstOrNull()?.let { noteId ->
            builder.setContentIntent(openNoteIntent(noteId))
        }
        return builder.build()
    }

    private fun baseNotification(): NotificationCompat.Builder =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(DesignR.drawable.ic_app_notification)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(launchAppIntent())

    private fun launchAppIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun openNoteIntent(noteId: Long): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launchIntent.putExtra(EXTRA_NOTE_ID, noteId)
        return PendingIntent.getActivity(
            this,
            noteNotificationId(noteId),
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun noteNotificationId(noteId: Long): Int =
        NOTE_NOTIFICATION_ID_BASE + (noteId % NOTE_NOTIFICATION_ID_RANGE).toInt()

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.recording_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NOTES_CHANNEL_ID,
                getString(R.string.recording_notification_notes_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    private data class ServiceState(
        val phase: SessionPhase,
        val isPaused: Boolean,
        val isProcessing: Boolean,
        val processingPercent: Int?,
    )

    companion object {
        const val EXTRA_NOTE_ID = "com.dmytrosamoilov.offhand.extra.NOTE_ID"

        private const val CHANNEL_ID = "recording"
        private const val NOTES_CHANNEL_ID = "finished_notes"
        private const val NOTIFICATION_ID = 1001
        private const val NOTE_NOTIFICATION_ID_BASE = 2000
        private const val NOTE_NOTIFICATION_ID_RANGE = 1000L
        private const val PROGRESS_MAX = 100
        private const val ACTION_START = "com.dmytrosamoilov.offhand.action.START_RECORDING"
        private const val ACTION_STOP = "com.dmytrosamoilov.offhand.action.STOP_RECORDING"
        private const val ACTION_PAUSE = "com.dmytrosamoilov.offhand.action.PAUSE_RECORDING"
        private const val ACTION_RESUME =
            "com.dmytrosamoilov.offhand.action.RESUME_RECORDING"
        private const val ACTION_RETRY_NOTE =
            "com.dmytrosamoilov.offhand.action.RETRY_NOTE"
        private const val EXTRA_RETRY_NOTE_ID =
            "com.dmytrosamoilov.offhand.extra.RETRY_NOTE_ID"
        private const val EXTRA_RETRY_AUDIO_FILE =
            "com.dmytrosamoilov.offhand.extra.RETRY_AUDIO_FILE"

        fun start(context: Context) {
            context.startForegroundService(serviceIntent(context, ACTION_START))
        }

        fun stop(context: Context) {
            context.startService(serviceIntent(context, ACTION_STOP))
        }

        fun retryNote(context: Context, noteId: Long, audioFileName: String) {
            context.startForegroundService(
                serviceIntent(context, ACTION_RETRY_NOTE)
                    .putExtra(EXTRA_RETRY_NOTE_ID, noteId)
                    .putExtra(EXTRA_RETRY_AUDIO_FILE, audioFileName),
            )
        }

        private fun serviceIntent(context: Context, action: String): Intent =
            Intent(context, RecordingService::class.java).setAction(action)
    }
}
