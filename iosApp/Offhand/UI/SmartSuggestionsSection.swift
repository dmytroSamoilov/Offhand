import EventKit
import EventKitUI
import OffhandShared
import SwiftUI

struct SmartSuggestionsSection: View {
    let viewModel: NotesViewModel
    let suggestions: SmartSuggestionsUi

    private static let cardWidth: CGFloat = 260
    private let cardBackground = Color(.secondarySystemGroupedBackground)

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "Smart suggestions"))
                .font(.footnote.weight(.semibold))
                .padding(.horizontal, 14)
                .frame(height: 32)
                .background(Brand.secondaryContainer, in: Capsule())
                .foregroundStyle(Brand.onSecondaryContainer)
                .padding(.horizontal, 20)
            content
        }
        .padding(.vertical, 20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(cardBackground, in: RoundedRectangle(cornerRadius: 12))
    }

    @ViewBuilder
    private var content: some View {
        switch onEnum(of: suggestions) {
        case .loading:
            HStack(spacing: 12) {
                ProgressView()
                Text(String(localized: "Looking for dates and appointments…")).foregroundStyle(.secondary)
            }
            .padding(.horizontal, 20)
        case .empty:
            Text(String(localized: "No suggestions for this note."))
                .foregroundStyle(.secondary)
                .padding(.horizontal, 20)
        case .notRun:
            Button {
                viewModel.onSuggestionsRequested()
            } label: {
                Label(String(localized: "Find suggestions"), systemImage: "calendar.badge.plus")
            }
            .buttonStyle(.bordered)
            .padding(.horizontal, 20)
        case .ready(let ready):
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(alignment: .top, spacing: 12) {
                    ForEach(ready.events, id: \.index) { event in
                        SuggestionCard(viewModel: viewModel, event: event)
                            .frame(width: Self.cardWidth)
                    }
                }
                .padding(.horizontal, 20)
            }
        }
    }
}

private struct SuggestionCard: View {
    let viewModel: NotesViewModel
    let event: CalendarEventUi

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(event.title).font(.headline).lineLimit(2)
            Text(event.isAllDay ? "\(event.whenText) · \(String(localized: "All day"))" : event.whenText)
                .font(.subheadline)
                .foregroundStyle(Brand.primary)
            if !event.location.isEmpty {
                Text(event.location).font(.footnote).foregroundStyle(.secondary).lineLimit(2)
            }
            if !event.details.isEmpty {
                Text(event.details).font(.footnote).foregroundStyle(.secondary).lineLimit(2)
            }
            Spacer(minLength: 4)
            actions
        }
        .padding(16)
        .frame(maxHeight: .infinity, alignment: .top)
        .background(Color(.tertiarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private var actions: some View {
        HStack {
            if event.isAdded {
                Label(String(localized: "Added"), systemImage: "checkmark")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Brand.primary)
            } else {
                Button(String(localized: "Add to calendar")) {
                    Haptics.confirm()
                    viewModel.onSuggestionAddRequested(index: event.index)
                }
                .buttonStyle(.bordered)
            }
            Spacer()
            Button {
                viewModel.onSuggestionDismissed(index: event.index)
            } label: {
                Image(systemName: "xmark")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(String(localized: "Dismiss suggestion"))
        }
    }
}

struct PendingCalendarEvent: Identifiable {
    let suggestion: CalendarEventSuggestion
    var id: String { "\(suggestion.title)-\(suggestion.startEpochMs)" }
}

// EKEventEditViewController needs no calendar authorisation on iOS 17+: the
// user reviews and saves the event inside the system editor.
struct CalendarEventEditor: UIViewControllerRepresentable {
    let suggestion: CalendarEventSuggestion
    let onFinished: (Bool) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(onFinished: onFinished) }

    func makeUIViewController(context: Context) -> EKEventEditViewController {
        let store = EKEventStore()
        let event = EKEvent(eventStore: store)
        event.title = suggestion.title
        event.startDate = Date(timeIntervalSince1970: Double(suggestion.startEpochMs) / 1000)
        event.endDate = Date(timeIntervalSince1970: Double(suggestion.endEpochMs) / 1000)
        event.isAllDay = suggestion.isAllDay
        event.location = suggestion.location.isEmpty ? nil : suggestion.location
        event.notes = suggestion.details.isEmpty ? nil : suggestion.details
        let controller = EKEventEditViewController()
        controller.eventStore = store
        controller.event = event
        controller.editViewDelegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ uiViewController: EKEventEditViewController, context: Context) {}

    final class Coordinator: NSObject, EKEventEditViewDelegate {
        private let onFinished: (Bool) -> Void

        init(onFinished: @escaping (Bool) -> Void) {
            self.onFinished = onFinished
        }

        func eventEditViewController(_ controller: EKEventEditViewController, didCompleteWith action: EKEventEditViewAction) {
            onFinished(action == .saved)
        }
    }
}
