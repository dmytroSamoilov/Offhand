package com.dmytrosamoilov.offhand

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioImportIntake
import com.dmytrosamoilov.offhand.feature.recording.service.RecordingService
import com.dmytrosamoilov.offhand.feature.settings.presentation.AudioImportPicker
import com.dmytrosamoilov.offhand.feature.settings.presentation.LocalAudioImportPicker
import com.dmytrosamoilov.offhand.feature.settings.presentation.SharedAudioImportViewModel
import com.dmytrosamoilov.offhand.root.RootScreen
import com.dmytrosamoilov.offhand.root.RootViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : FragmentActivity() {

    private var requestedNoteId by mutableStateOf<Long?>(null)
    private val analyticsTracker: AnalyticsTracker by inject()
    private val importIntake: AudioImportIntake by inject()
    // Activity-scoped so the dialog host inside RootScreen observes the same instance.
    private val sharedAudioImport: SharedAudioImportViewModel by viewModel()
    // Registered on the activity, not in Settings: the picker sends the app to
    // the background, the lock screen replaces Settings, and a launcher there
    // would be gone when the result comes back.
    private val audioImportPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        importAudio(uris)
    }
    private val singleAudioImportPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importAudio(listOfNotNull(uri))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!FrameworkCompatibility.isComposeSupported()) {
            showUnsupportedFramework()
            return
        }
        enableEdgeToEdge()
        consumeNoteIdExtra(intent)
        consumeSharedAudio(intent)
        setContent {
            val viewModel: RootViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            OffhandTheme(dynamicColor = state.isDynamicColorEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CompositionLocalProvider(
                        LocalAudioImportPicker provides AudioImportPicker { allowMultiple -> openImportPicker(allowMultiple) },
                    ) {
                        RootScreen(
                            viewModel = viewModel,
                            requestedNoteId = requestedNoteId,
                            onRequestedNoteConsumed = { requestedNoteId = null },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeNoteIdExtra(intent)
        consumeSharedAudio(intent)
    }

    private fun consumeNoteIdExtra(intent: Intent?) {
        val noteId = intent?.getLongExtra(RecordingService.EXTRA_NOTE_ID, NO_NOTE_ID)
        if (noteId != null && noteId != NO_NOTE_ID) {
            requestedNoteId = noteId
            intent.removeExtra(RecordingService.EXTRA_NOTE_ID)
        }
    }

    private fun openImportPicker(allowMultiple: Boolean) {
        if (allowMultiple) audioImportPicker.launch(IMPORT_MIME_TYPES) else singleAudioImportPicker.launch(IMPORT_MIME_TYPES)
    }

    private fun consumeSharedAudio(intent: Intent?) {
        val type = intent?.type ?: return
        if (!type.startsWith(AUDIO_MIME_PREFIX) && !type.startsWith(VIDEO_MIME_PREFIX)) return
        val uris = sharedAudioUris(intent)
        intent.removeExtra(Intent.EXTRA_STREAM)
        importAudio(uris)
    }

    private fun importAudio(uris: List<Uri>) {
        if (uris.isEmpty()) return
        lifecycleScope.launch {
            val staged = uris.map { uri -> importIntake.stage(uri) }
            sharedAudioImport.onSharedAudioReceived(
                sources = staged.filterNotNull(),
                unreadableCount = staged.count { it == null },
            )
        }
    }

    private fun sharedAudioUris(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
        Intent.ACTION_SEND_MULTIPLE ->
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        else -> emptyList()
    }

    private companion object {
        const val NO_NOTE_ID = -1L
        const val AUDIO_MIME_PREFIX = "audio/"
        const val VIDEO_MIME_PREFIX = "video/"
        val IMPORT_MIME_TYPES = arrayOf("audio/*", "video/*")
    }

    private fun showUnsupportedFramework() {
        analyticsTracker.track(AnalyticsEvents.frameworkUnsupported())
        AlertDialog.Builder(this)
            .setTitle(R.string.framework_unsupported_title)
            .setMessage(R.string.framework_unsupported_body)
            .setPositiveButton(R.string.framework_unsupported_close) { _, _ -> finish() }
            .setOnDismissListener { finish() }
            .show()
    }
}
