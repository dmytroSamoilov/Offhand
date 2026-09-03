import ActivityKit
import Foundation

struct NoteActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        enum Phase: String, Codable {
            case recording
            case paused
            case processing
            case finished
            case openApp
        }

        var phase: Phase
        var startedAt: Date
        var progressPercent: Int?
    }
}
