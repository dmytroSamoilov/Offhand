import os

// Temporary testing aid. DEV_UNLOCK is defined only for the Debug-dev
// configuration in project.yml, so on every other configuration these branches
// are compiled out entirely rather than merely evaluating to false.
enum DevFlags {

    #if DEV_UNLOCK
    static let skipsAppLock = true
    #else
    static let skipsAppLock = false
    #endif

    static func warnIfWeakened() {
        guard skipsAppLock else { return }
        os.Logger(subsystem: "com.dmytrosamoilov.offhand", category: "DevFlags")
            .warning("DEV_UNLOCK build: the app lock is skipped. Debug-dev only.")
    }
}
