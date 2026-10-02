import Foundation
import Network
import OffhandShared

// Reports whether the current path is one the user is not paying per byte for:
// satisfied, not expensive (cellular, hotspot) and not constrained (Low Data
// Mode). The monitor runs for the life of the process.
final class NetworkMonitorBridgeImpl: IosNetworkMonitorBridge {

    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.dmytrosamoilov.offhand.network-monitor")
    private var isUnmeteredNow = false

    init() {
        isUnmeteredNow = Self.isUnmetered(monitor.currentPath)
    }

    func isUnmetered() -> Bool { isUnmeteredNow }

    func observe(onChange: @escaping (KotlinBoolean) -> Void) {
        monitor.pathUpdateHandler = { [weak self] path in
            let unmetered = Self.isUnmetered(path)
            self?.isUnmeteredNow = unmetered
            onChange(KotlinBoolean(value: unmetered))
        }
        monitor.start(queue: queue)
    }

    private static func isUnmetered(_ path: NWPath) -> Bool {
        path.status == .satisfied && !path.isExpensive && !path.isConstrained
    }
}
