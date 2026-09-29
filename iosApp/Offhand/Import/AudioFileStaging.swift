import Foundation
import OffhandShared

// A picked or shared file is only readable while its grant lasts, while decoding
// runs later on the session's scope, so it is copied into tmp first; the
// decoder deletes the copy when it is done. Files iOS dropped into
// Documents/Inbox for us are removed once copied, they are ours to clean up.
enum AudioFileStaging {
    static func stage(_ url: URL) -> AudioImportSource? {
        let accessed = url.startAccessingSecurityScopedResource()
        defer { if accessed { url.stopAccessingSecurityScopedResource() } }
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("imports", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let copy = directory.appendingPathComponent(UUID().uuidString).appendingPathExtension(url.pathExtension)
        guard (try? FileManager.default.copyItem(at: url, to: copy)) != nil else { return nil }
        if url.pathComponents.contains("Inbox") {
            try? FileManager.default.removeItem(at: url)
        }
        return AudioImportSource(handle: copy.path, displayName: url.lastPathComponent)
    }
}
