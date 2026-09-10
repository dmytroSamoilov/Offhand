package com.dmytrosamoilov.offhand

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.notes.R as NotesR
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioImportIntake
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ImportAudioResult
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ImportAudioUseCase
import com.dmytrosamoilov.offhand.feature.recording.service.RecordingService
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import com.dmytrosamoilov.offhand.root.RootScreen
import com.dmytrosamoilov.offhand.root.RootViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : FragmentActivity() {

    private var requestedNoteId by mutableStateOf<Long?>(null)
    private val importIntake: AudioImportIntake by inject()
    private val importAudio: ImportAudioUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
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
                    RootScreen(
                        viewModel = viewModel,
                        requestedNoteId = requestedNoteId,
                        onRequestedNoteConsumed = { requestedNoteId = null },
                    )
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

    private fun consumeSharedAudio(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type?.startsWith(AUDIO_MIME_PREFIX) != true) return
        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return
        intent.removeExtra(Intent.EXTRA_STREAM)
        lifecycleScope.launch {
            val source = importIntake.stage(uri)
            val locked = source != null && importAudio(source) == ImportAudioResult.LOCKED
            if (source == null) showToast(NotesR.string.notes_import_error_unreadable)
            if (locked) showToast(NotesR.string.notes_import_locked)
        }
    }

    private fun showToast(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val NO_NOTE_ID = -1L
        const val AUDIO_MIME_PREFIX = "audio/"
    }
}
