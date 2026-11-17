package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.domain.SessionPhase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

// What the home-screen widget needs to draw itself and to know what a tap
// may do. The app hands it to the widget through the shared app group.
data class RecordWidgetFolder(val id: Long, val name: String)

data class RecordWidgetState(
    val folders: List<RecordWidgetFolder>,
    val isPro: Boolean,
    val isRecording: Boolean,
)

class IosRecordWidgetState(
    foldersRepository: FoldersRepository,
    proStatusRepository: ProStatusRepository,
    sessionManager: RecordingSessionManager,
) {
    val state: Flow<RecordWidgetState> = combine(
        foldersRepository.observeFolders(),
        proStatusRepository.observeStatus(),
        sessionManager.session,
    ) { folders, status, session ->
        RecordWidgetState(
            folders = folders.map { RecordWidgetFolder(it.id, it.name) },
            isPro = status.isPro,
            isRecording = session.phase == SessionPhase.RECORDING,
        )
    }.distinctUntilChanged()
}
