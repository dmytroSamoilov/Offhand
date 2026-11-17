import AppIntents
import SwiftUI
import WidgetKit

// The widget's settings: the folder new recordings go to. Declared only in
// the extension, which is where iOS runs the folder list for the edit sheet.
struct WidgetFolderEntity: AppEntity {
    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Folder"
    static let defaultQuery = WidgetFolderQuery()

    let id: String
    let name: String

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(name)")
    }
}

struct WidgetFolderQuery: EntityQuery {
    func entities(for identifiers: [String]) async throws -> [WidgetFolderEntity] {
        all().filter { identifiers.contains($0.id) }
    }

    func suggestedEntities() async throws -> [WidgetFolderEntity] {
        all()
    }

    private func all() -> [WidgetFolderEntity] {
        RecordWidgetShared.load().folders.map { WidgetFolderEntity(id: String($0.id), name: $0.name) }
    }
}

struct RecordWidgetConfigurationIntent: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "Record a note"
    static let description = IntentDescription("Choose the folder new recordings are saved to.")

    @Parameter(title: "Folder")
    var folder: WidgetFolderEntity?
}

struct RecordWidgetEntry: TimelineEntry {
    let date: Date
    let snapshot: RecordWidgetSnapshot
    let folder: RecordWidgetFolderInfo?
}

// One entry is enough: the app reloads the widget whenever what it shows
// changes, a recording starting or stopping included.
struct RecordWidgetProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> RecordWidgetEntry {
        RecordWidgetEntry(date: .now, snapshot: RecordWidgetSnapshot(), folder: nil)
    }

    func snapshot(for configuration: RecordWidgetConfigurationIntent, in context: Context) async -> RecordWidgetEntry {
        entry(for: configuration)
    }

    func timeline(for configuration: RecordWidgetConfigurationIntent, in context: Context) async -> Timeline<RecordWidgetEntry> {
        Timeline(entries: [entry(for: configuration)], policy: .never)
    }

    // The folder is looked up by id every time, so a rename shows up and a
    // deleted folder falls back to no folder.
    private func entry(for configuration: RecordWidgetConfigurationIntent) -> RecordWidgetEntry {
        let snapshot = RecordWidgetShared.load()
        let folderId = configuration.folder.flatMap { Int64($0.id) }
        return RecordWidgetEntry(date: .now, snapshot: snapshot, folder: snapshot.folders.first { $0.id == folderId })
    }
}

struct RecordWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: RecordWidgetShared.kind,
            intent: RecordWidgetConfigurationIntent.self,
            provider: RecordWidgetProvider()
        ) { entry in
            RecordWidgetView(entry: entry)
        }
        .configurationDisplayName(Text(String(localized: "Record a note")))
        .description(Text(String(localized: "Start or stop a recording from your Home Screen.")))
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

// Record opens the app with the widget's folder and the app starts the
// recording there; Stop stops it without opening the app.
private struct RecordWidgetView: View {
    let entry: RecordWidgetEntry

    private static let idle = Color(red: 11 / 255, green: 87 / 255, blue: 208 / 255)
    private static let recording = Color(red: 179 / 255, green: 38 / 255, blue: 30 / 255)

    var body: some View {
        content
            .containerBackground(entry.snapshot.isRecording ? Self.recording : Self.idle, for: .widget)
    }

    @ViewBuilder
    private var content: some View {
        if entry.snapshot.isRecording {
            Button(intent: StopWidgetRecordingIntent()) { label }
                .buttonStyle(.plain)
        } else {
            label.widgetURL(RecordWidgetShared.recordURL(folderId: entry.folder?.id))
        }
    }

    private var label: some View {
        VStack(alignment: .leading, spacing: 6) {
            Image(systemName: entry.snapshot.isRecording ? "stop.fill" : "mic.fill")
                .font(.title)
            Spacer(minLength: 0)
            Text(entry.snapshot.isRecording ? String(localized: "Stop") : String(localized: "Record"))
                .font(.headline)
            Text(entry.folder?.name ?? String(localized: "No folder"))
                .font(.caption)
                .opacity(0.8)
                .lineLimit(1)
        }
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .contentShape(Rectangle())
    }
}
