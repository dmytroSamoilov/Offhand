package com.dmytrosamoilov.offhand.feature.onboarding.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.ModelState
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.designsystem.R as DesignR
import com.dmytrosamoilov.offhand.feature.onboarding.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ModelDownloadService : Service() {

    @Inject
    lateinit var modelManager: ModelManager

    @Inject
    lateinit var speechToText: SpeechToText

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var downloadJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            startDownload()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startDownload() {
        if (downloadJob != null) return
        startForeground(
            NOTIFICATION_ID,
            progressNotification(percent = null),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        observeProgress()
        downloadJob = serviceScope.launch {
            try {
                speechToText.prepare()
                speechToText.release()
                modelManager.ensureModelAvailable()
                notifyFinished(isSuccess = true)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                Timber.tag(LOG_TAG).e(t, "Model download failed")
                notifyFinished(isSuccess = false)
            } finally {
                ServiceCompat.stopForeground(
                    this@ModelDownloadService,
                    ServiceCompat.STOP_FOREGROUND_REMOVE,
                )
                stopSelf()
            }
        }
    }

    private fun observeProgress() {
        modelManager.modelState
            .map { state -> (state as? ModelState.Downloading)?.toPercent() }
            .distinctUntilChanged()
            .onEach { percent ->
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, progressNotification(percent))
            }
            .launchIn(serviceScope)
    }

    private fun ModelState.Downloading.toPercent(): Int =
        (progress * 100).toInt().coerceIn(0, 100)

    private fun progressNotification(percent: Int?): Notification {
        val builder = NotificationCompat.Builder(this, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(DesignR.drawable.ic_app_notification)
            .setContentTitle(getString(R.string.onboarding_notification_downloading_title))
            .setContentText(getString(R.string.onboarding_notification_downloading_text))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(launchAppIntent())
        if (percent != null) {
            builder.setProgress(PROGRESS_MAX, percent, false)
        } else {
            builder.setProgress(PROGRESS_MAX, 0, true)
        }
        return builder.build()
    }

    private fun notifyFinished(isSuccess: Boolean) {
        val notification = NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
            .setSmallIcon(DesignR.drawable.ic_app_notification)
            .setContentTitle(
                getString(
                    if (isSuccess) {
                        R.string.onboarding_notification_ready_title
                    } else {
                        R.string.onboarding_notification_failed_title
                    },
                ),
            )
            .setContentText(
                getString(
                    if (isSuccess) {
                        R.string.onboarding_notification_ready_text
                    } else {
                        R.string.onboarding_notification_failed_text
                    },
                ),
            )
            .setAutoCancel(true)
            .setContentIntent(launchAppIntent())
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(STATUS_NOTIFICATION_ID, notification)
    }

    private fun launchAppIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                getString(R.string.onboarding_notification_download_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                STATUS_CHANNEL_ID,
                getString(R.string.onboarding_notification_status_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    companion object {
        private const val LOG_TAG = "ModelDownload"
        private const val DOWNLOAD_CHANNEL_ID = "model_download"
        private const val STATUS_CHANNEL_ID = "model_status"
        private const val NOTIFICATION_ID = 3001
        private const val STATUS_NOTIFICATION_ID = 3002
        private const val PROGRESS_MAX = 100
        private const val ACTION_START = "com.dmytrosamoilov.offhand.action.START_MODEL_DOWNLOAD"

        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, ModelDownloadService::class.java).setAction(ACTION_START),
            )
        }
    }
}
