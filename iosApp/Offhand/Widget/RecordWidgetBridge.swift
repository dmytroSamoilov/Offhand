import Foundation
import OffhandShared
import WidgetKit

// The app's side of the home-screen widget: it keeps the widget's snapshot
// current and stops a recording when the widget's Stop is tapped. Starting
// always goes through the app (the widget's Record opens it with the folder),
// so the paywall, the microphone request and the record sheet all show as
// they do from inside the app.
@MainActor
final class RecordWidgetBridge {
    static let shared = RecordWidgetBridge()

    func start() {
        RecordWidgetActions.stop = { [weak self] in self?.stopRecording() }
        Task { [weak self] in
            for await state in SharedGraph.shared.recordWidgetState() {
                self?.publish(state)
            }
        }
    }

    private func publish(_ state: RecordWidgetState) {
        let snapshot = RecordWidgetSnapshot(
            folders: state.folders.map { RecordWidgetFolderInfo(id: $0.id, name: $0.name) },
            isRecording: state.isRecording
        )
        guard snapshot != RecordWidgetShared.load() else { return }
        RecordWidgetShared.save(snapshot)
        WidgetCenter.shared.reloadTimelines(ofKind: RecordWidgetShared.kind)
    }

    private func stopRecording() {
        guard SharedGraph.shared.sessionManager().session.value.phase == .recording else { return }
        AppViewModels.recording.onStopRecording()
    }
}
