import EventKit
import EventKitUI
import OffhandShared
import SwiftUI

struct SmartSuggestionsSection: View {
    let viewModel: NotesViewModel
    let suggestions: SmartSuggestionsUi

    private static let cardWidth: CGFloat = 260

    var body: some View {
        VStack(alignment: .leading, spacing: OffhandSpacing.controls) {
            HStack(spacing: OffhandSpacing.related) {
                SectionLabel(String(localized: "Smart suggestions"))
                if case .locked = onEnum(of: suggestions) { ProBadge() }
            }
            content
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private var content: some View {
        switch onEnum(of: suggestions) {
        case .loading:
            HStack(spacing: OffhandSpacing.controls) {
                ProgressView()
                    .tint(OffhandColor.ink2)
                message(String(localized: "Looking for dates and appointments…"))
            }
        case .empty:
            message(String(localized: "No suggestions for this note."))
        case .notRun:
            findButton
        case .locked:
            VStack(alignment: .leading, spacing: OffhandSpacing.controls) {
                message(String(localized: "Offhand Pro finds calendar events and to-dos in your notes."))
                findButton
            }
        case .ready(let ready):
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(alignment: .top, spacing: OffhandSpacing.controls) {
                    ForEach(ready.events, id: \.index) { event in
                        SuggestionCard(viewModel: viewModel, event: event)
                            .frame(width: Self.cardWidth)
                    }
                }
                .padding(.horizontal, OffhandSpacing.screen)
            }
            .padding(.horizontal, -OffhandSpacing.screen)
        }
    }

    private func message(_ text: String) -> some View {
        Text(text)
            .offhandText(.bodySmall)
            .foregroundStyle(OffhandColor.ink2)
    }

    private var findButton: some View {
        SecondaryButton(String(localized: "Find suggestions"), systemImage: "calendar.badge.plus", height: 48) {
            viewModel.onSuggestionsRequested()
        }
    }
}

private struct SuggestionCard: View {
    let viewModel: NotesViewModel
    let event: CalendarEventUi

    var body: some View {
        SurfaceCard {
            VStack(alignment: .leading, spacing: 6) {
                Text(event.title)
                    .offhandText(.rowTitle)
                    .foregroundStyle(OffhandColor.ink)
                    .lineLimit(2)
                Text(event.isAllDay ? "\(event.whenText) · \(String(localized: "All day"))" : event.whenText)
                    .offhandText(.bodySmall, face: .interMedium)
                    .foregroundStyle(OffhandColor.ink)
                if !event.location.isEmpty {
                    detail(event.location)
                }
                if !event.details.isEmpty {
                    detail(event.details)
                }
                Spacer(minLength: 4)
                actions
            }
            .padding(OffhandSpacing.card)
            .frame(maxHeight: .infinity, alignment: .top)
        }
    }

    private func detail(_ text: String) -> some View {
        Text(text)
            .offhandText(.caption)
            .foregroundStyle(OffhandColor.ink2)
            .lineLimit(2)
    }

    private var actions: some View {
        HStack(spacing: OffhandSpacing.related) {
            if event.isAdded {
                Label(String(localized: "Added"), systemImage: "checkmark")
                    .offhandText(.buttonSmall)
                    .foregroundStyle(OffhandColor.ink2)
                    .frame(minHeight: OffhandSpacing.touchTarget)
            } else {
                SecondaryButton(String(localized: "Add to calendar"), height: OffhandSpacing.touchTarget) {
                    Haptics.confirm()
                    viewModel.onSuggestionAddRequested(index: event.index)
                }
            }
            Spacer(minLength: 0)
            BarIconButton(
                systemImage: "xmark",
                label: String(localized: "Dismiss suggestion"),
                tint: OffhandColor.ink3
            ) {
                viewModel.onSuggestionDismissed(index: event.index)
            }
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
