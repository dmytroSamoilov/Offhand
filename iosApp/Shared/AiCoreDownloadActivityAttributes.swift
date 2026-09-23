import ActivityKit
import Foundation

struct AiCoreDownloadActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        enum Phase: String, Codable {
            case downloading
            case ready
            case paused
        }

        var phase: Phase
        var progressPercent: Int
    }
}
