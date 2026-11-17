import AppIntents
import Foundation
import WidgetKit

// The record widget lives in the widget extension and the recording in the
// app, so the two meet here: a snapshot the app writes into the shared app
// group for the widget to draw from, the link Record opens the app with, and
// the intent Stop runs.
// The folder choice in the widget's settings is the extension's alone and
// lives in RecordWidget.swift.

struct RecordWidgetFolderInfo: Codable, Hashable {
    let id: Int64
    let name: String
}

struct RecordWidgetSnapshot: Codable, Equatable {
    var folders: [RecordWidgetFolderInfo] = []
    var isRecording = false
}

enum RecordWidgetShared {
    static let kind = "RecordWidget"
    static let recordHost = "record"
    static let folderQueryItem = "folder"
    private static let snapshotKey = "record_widget_snapshot"

    static let appGroup = Bundle.main.object(forInfoDictionaryKey: "OffhandAppGroup") as? String ?? ""
    static let urlScheme = Bundle.main.object(forInfoDictionaryKey: "OffhandURLScheme") as? String ?? ""

    static func load() -> RecordWidgetSnapshot {
        guard let data = UserDefaults(suiteName: appGroup)?.data(forKey: snapshotKey),
              let snapshot = try? JSONDecoder().decode(RecordWidgetSnapshot.self, from: data) else {
            return RecordWidgetSnapshot()
        }
        return snapshot
    }

    static func save(_ snapshot: RecordWidgetSnapshot) {
        guard let data = try? JSONEncoder().encode(snapshot) else { return }
        UserDefaults(suiteName: appGroup)?.set(data, forKey: snapshotKey)
    }

    static func recordURL(folderId: Int64?) -> URL? {
        var components = URLComponents()
        components.scheme = urlScheme
        components.host = recordHost
        if let folderId {
            components.queryItems = [URLQueryItem(name: folderQueryItem, value: String(folderId))]
        }
        return components.url
    }
}

// Set by the app when it starts; nil inside the widget extension, where the
// intent is only declared. A LiveActivityIntent runs in the app's process, so
// the closure is the app's.
@MainActor
enum RecordWidgetActions {
    static var stop: (() async -> Void)?
}

// A LiveActivityIntent so iOS runs it in the app, where the recording is.
// Stopping needs no microphone, so the background audio rules do not apply.
struct StopWidgetRecordingIntent: LiveActivityIntent {
    static let title: LocalizedStringResource = "Stop recording"
    static let isDiscoverable = false

    @MainActor
    func perform() async throws -> some IntentResult {
        await RecordWidgetActions.stop?()
        return .result()
    }
}
