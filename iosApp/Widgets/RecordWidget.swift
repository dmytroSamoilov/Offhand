import AppIntents
import SwiftUI
import WidgetKit

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

private struct RecordWidgetView: View {
    let entry: RecordWidgetEntry
    @Environment(\.colorScheme) private var systemScheme
    @Environment(\.widgetFamily) private var family

    private var isRecording: Bool { entry.snapshot.isRecording }

    private var scheme: ColorScheme {
        guard isRecording else { return systemScheme }
        return systemScheme == .dark ? .light : .dark
    }

    var body: some View {
        content
            .foregroundStyle(OffhandColor.ink)
            .environment(\.colorScheme, scheme)
            .containerBackground(for: .widget) {
                (isRecording ? OffhandColor.ground : OffhandColor.surface).environment(\.colorScheme, scheme)
            }
    }

    @ViewBuilder
    private var content: some View {
        if isRecording {
            Button(intent: StopWidgetRecordingIntent()) { layout }
                .buttonStyle(.plain)
                .accessibilityLabel(String(localized: "Stop recording"))
        } else {
            layout
                .widgetURL(RecordWidgetShared.recordURL(folderId: entry.folder?.id))
                .accessibilityLabel(String(localized: "Record a note"))
        }
    }

    @ViewBuilder
    private var layout: some View {
        if family == .systemMedium {
            HStack(spacing: 14) {
                glyph
                VStack(alignment: .leading, spacing: 2) {
                    Text(isRecording ? String(localized: "Stop and save") : String(localized: "Record"))
                        .offhandText(.rowTitle)
                    caption
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if !isRecording {
                    WidgetWave().frame(width: 64, height: 28)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        } else {
            VStack(alignment: .leading, spacing: 0) {
                glyph
                Spacer(minLength: 0)
                Text(isRecording ? String(localized: "Stop") : String(localized: "Record"))
                    .font(.offhand(.tightSemibold, size: 24, relativeTo: .title2))
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                caption.padding(.top, 2)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        }
    }

    private var glyph: some View {
        Circle()
            .fill(OffhandColor.ink)
            .frame(width: 44, height: 44)
            .overlay {
                if isRecording {
                    RoundedRectangle(cornerRadius: 3, style: .continuous)
                        .fill(OffhandColor.ground)
                        .frame(width: 14, height: 14)
                } else {
                    Circle()
                        .fill(OffhandColor.record)
                        .frame(width: 12, height: 12)
                }
            }
            .accessibilityHidden(true)
    }

    @ViewBuilder
    private var caption: some View {
        if let text = captionText {
            Text(text)
                .offhandText(.meta)
                .foregroundStyle(isRecording ? OffhandColor.danger : OffhandColor.ink3)
                .lineLimit(1)
        }
    }

    private var captionText: String? {
        switch (isRecording, entry.folder?.name) {
        case (true, let name?): return String(format: String(localized: "Recording to %@"), name)
        case (true, nil): return String(localized: "Recording")
        case (false, let name?): return String(format: String(localized: "To %@"), name)
        case (false, nil): return nil
        }
    }
}

private struct WidgetWave: View {
    private static let bars: [CGFloat] = [0.3, 0.6, 0.45, 0.85, 0.5, 1, 0.65, 0.4, 0.7, 0.35, 0.55]

    var body: some View {
        Canvas { context, size in
            let step = size.width / CGFloat(Self.bars.count)
            for (index, level) in Self.bars.enumerated() {
                let x = step * (CGFloat(index) + 0.5)
                let height = level * size.height
                var path = Path()
                path.move(to: CGPoint(x: x, y: (size.height - height) / 2))
                path.addLine(to: CGPoint(x: x, y: (size.height + height) / 2))
                context.stroke(path, with: .color(OffhandColor.wave), style: StrokeStyle(lineWidth: 2.5, lineCap: .round))
            }
        }
        .accessibilityHidden(true)
    }
}
