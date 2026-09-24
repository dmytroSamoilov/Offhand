import Foundation
import OffhandShared

enum ImportNoticeText {
    static func title(_ notice: ImportNoticeUi?) -> String {
        if case .started = onEnum(of: notice) {
            return String(localized: "Import started")
        }
        return String(localized: "Import audio")
    }

    static func message(_ notice: ImportNoticeUi) -> String {
        switch onEnum(of: notice) {
        case .started(let started):
            return String.localizedStringWithFormat(
                String(localized: "%d files are being imported. The notes are created in the background and will show up in your notes list as soon as they are ready."),
                started.fileCount
            )
        case .unreadable:
            return String(localized: "Some of the files could not be read.")
        }
    }
}
