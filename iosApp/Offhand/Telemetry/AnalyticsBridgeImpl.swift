import FirebaseAnalytics
import FirebaseCore
import Foundation
import OffhandShared

// The shared tracker has already checked consent; this end only makes sure
// Firebase exists, which it does not until consent was granted.
final class AnalyticsBridgeImpl: IosAnalyticsBridge {

    func logEvent(name: String, params: [String: Any]) {
        guard FirebaseApp.app() != nil else { return }
        Analytics.logEvent(name, parameters: params)
    }
}
