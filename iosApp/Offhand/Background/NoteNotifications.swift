import Foundation
import UserNotifications

// Android pings the user when a note finishes and opens that note when the
// notification is tapped. This is the iOS half of that loop.
final class NoteNotifications: NSObject, ObservableObject, UNUserNotificationCenterDelegate {

    static let shared = NoteNotifications()

    @Published var noteIdToOpen: Int64?

    private static let noteIdKey = "noteId"

    func register() {
        UNUserNotificationCenter.current().delegate = self
    }

    func noteReady(noteId: Int64) {
        post(
            noteId: noteId,
            title: String(localized: "Your note is ready"),
            body: String(localized: "Tap to read what Offhand wrote up.")
        )
    }

    func noteFailed(noteId: Int64) {
        post(
            noteId: noteId,
            title: String(localized: "That note needs another try"),
            body: String(localized: "Tap to open it and re-transcribe.")
        )
    }

    private func post(noteId: Int64, title: String, body: String) {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
            guard granted else { return }
            let content = UNMutableNotificationContent()
            content.title = title
            content.body = body
            content.userInfo = [Self.noteIdKey: NSNumber(value: noteId)]
            // Deliver immediately: a triggered request stays pending, and pending
            // requests get cleared when the app next becomes active.
            center.add(
                UNNotificationRequest(
                    identifier: "note-ready-\(noteId)",
                    content: content,
                    trigger: nil
                )
            )
        }
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let noteId = (response.notification.request.content.userInfo[Self.noteIdKey] as? NSNumber)?
            .int64Value
        Task { @MainActor in
            if let noteId {
                Self.shared.noteIdToOpen = noteId
            }
            completionHandler()
        }
    }

    // Android surfaces this notification whether or not its app is in front, so
    // match that rather than silently swallowing it in the foreground.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }
}
