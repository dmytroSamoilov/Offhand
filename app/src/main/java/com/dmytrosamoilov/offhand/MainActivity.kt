package com.dmytrosamoilov.offhand

import android.content.Intent
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
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.recording.service.RecordingService
import com.dmytrosamoilov.offhand.root.RootScreen
import com.dmytrosamoilov.offhand.root.RootViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private var requestedNoteId by mutableStateOf<Long?>(null)

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
        setContent {
            val viewModel: RootViewModel = hiltViewModel()
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
    }

    private fun consumeNoteIdExtra(intent: Intent?) {
        val noteId = intent?.getLongExtra(RecordingService.EXTRA_NOTE_ID, NO_NOTE_ID)
        if (noteId != null && noteId != NO_NOTE_ID) {
            requestedNoteId = noteId
            intent.removeExtra(RecordingService.EXTRA_NOTE_ID)
        }
    }

    private companion object {
        const val NO_NOTE_ID = -1L
    }
}
