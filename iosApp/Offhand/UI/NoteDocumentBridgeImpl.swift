import CoreText
import OffhandShared
import UIKit

// Draws the shared NoteDocument as an A4 PDF: one attributed string paginated
// with CoreText, and the source footer pinned to the bottom of the last page.
final class NoteDocumentBridgeImpl: IosNoteDocumentBridge {
    private static let pageSize = CGSize(width: 595, height: 842)
    private static let margin: CGFloat = 48
    private static let footerHeight: CGFloat = 30
    private static let iconSize: CGFloat = 20
    private static let iconGap: CGFloat = 8
    private static let detailColor = UIColor(white: 0.4, alpha: 1)

    func appIconPng() -> Data? {
        guard let icons = Bundle.main.infoDictionary?["CFBundleIcons"] as? [String: Any],
              let primary = icons["CFBundlePrimaryIcon"] as? [String: Any],
              let files = primary["CFBundleIconFiles"] as? [String],
              let name = files.last,
              let image = UIImage(named: name)
        else { return nil }
        return image.pngData()
    }

    func writePdf(document: NoteDocument, iconPng: Data?, path: String) -> Bool {
        let text = attributedText(for: document)
        let renderer = UIGraphicsPDFRenderer(bounds: CGRect(origin: .zero, size: Self.pageSize))
        do {
            try renderer.writePDF(to: URL(fileURLWithPath: path)) { context in
                let lastBottom = drawPages(text, in: context)
                drawFooter(document.footer, iconPng: iconPng, after: lastBottom, in: context)
            }
            return true
        } catch {
            return false
        }
    }

    private var contentRect: CGRect {
        CGRect(origin: .zero, size: Self.pageSize).insetBy(dx: Self.margin, dy: Self.margin)
    }

    // Returns the y position below the last drawn line on the last page.
    private func drawPages(_ text: NSAttributedString, in context: UIGraphicsPDFRendererContext) -> CGFloat {
        let framesetter = CTFramesetterCreateWithAttributedString(text)
        var range = CFRange(location: 0, length: 0)
        var lastBottom = Self.margin
        repeat {
            context.beginPage()
            let path = CGPath(rect: contentRect, transform: nil)
            let frame = CTFramesetterCreateFrame(framesetter, range, path, nil)
            flipped(context.cgContext) { CTFrameDraw(frame, $0) }
            lastBottom = bottomOfLines(in: frame)
            let visible = CTFrameGetVisibleStringRange(frame)
            range = CFRange(location: visible.location + visible.length, length: 0)
        } while range.location < text.length
        return lastBottom
    }

    private func bottomOfLines(in frame: CTFrame) -> CGFloat {
        let lines = CTFrameGetLines(frame) as NSArray
        guard lines.count > 0 else { return Self.margin }
        var origins = [CGPoint](repeating: .zero, count: lines.count)
        CTFrameGetLineOrigins(frame, CFRange(location: 0, length: lines.count), &origins)
        var descent: CGFloat = 0
        CTLineGetTypographicBounds(lines[lines.count - 1] as! CTLine, nil, &descent, nil)
        return Self.pageSize.height - (contentRect.minY + origins[lines.count - 1].y) + descent
    }

    private func drawFooter(_ footer: DocumentFooter, iconPng: Data?, after lastBottom: CGFloat, in context: UIGraphicsPDFRendererContext) {
        let top = Self.pageSize.height - Self.margin - Self.footerHeight
        if lastBottom > top { context.beginPage() }
        var textX = Self.margin
        if let iconPng, let icon = UIImage(data: iconPng) {
            icon.draw(in: CGRect(x: Self.margin, y: top, width: Self.iconSize, height: Self.iconSize))
            textX += Self.iconSize + Self.iconGap
        }
        let attributes: [NSAttributedString.Key: Any] = [.font: UIFont.systemFont(ofSize: 9), .foregroundColor: Self.detailColor]
        footer.sourceLine.draw(at: CGPoint(x: textX, y: top), withAttributes: attributes)
        footer.exportedLine.draw(at: CGPoint(x: textX, y: top + 12), withAttributes: attributes)
    }

    private func flipped(_ cg: CGContext, _ draw: (CGContext) -> Void) {
        cg.saveGState()
        cg.textMatrix = .identity
        cg.translateBy(x: 0, y: Self.pageSize.height)
        cg.scaleBy(x: 1, y: -1)
        draw(cg)
        cg.restoreGState()
    }

    private func attributedText(for document: NoteDocument) -> NSAttributedString {
        let text = NSMutableAttributedString()
        text.append(line(document.title, font: .boldSystemFont(ofSize: 20), spacing: 4))
        for detail in document.details {
            text.append(line("\(detail.label): \(detail.value)", font: .systemFont(ofSize: 9), color: Self.detailColor, spacing: 1))
        }
        for section in document.sections {
            text.append(line(section.heading, font: .boldSystemFont(ofSize: 14), spacing: 6, before: 14))
            for block in section.blocks { text.append(attributed(block)) }
        }
        return text
    }

    private func attributed(_ block: DocumentBlock) -> NSAttributedString {
        switch onEnum(of: block) {
        case .heading(let heading):
            return line(heading.text, font: .boldSystemFont(ofSize: 12), spacing: 4, before: 8)
        case .paragraph(let paragraph):
            return line(paragraph.text, font: .systemFont(ofSize: 11), spacing: 6)
        case .bullet(let bullet):
            return line("•  \(bullet.text)", font: .systemFont(ofSize: 11), spacing: 3, indent: 14)
        case .numbered(let numbered):
            return line("\(numbered.number).  \(numbered.text)", font: .systemFont(ofSize: 11), spacing: 3, indent: 14)
        }
    }

    private func line(
        _ text: String,
        font: UIFont,
        color: UIColor = .black,
        spacing: CGFloat,
        before: CGFloat = 0,
        indent: CGFloat = 0
    ) -> NSAttributedString {
        let style = NSMutableParagraphStyle()
        style.paragraphSpacing = spacing
        style.paragraphSpacingBefore = before
        style.headIndent = indent
        style.lineHeightMultiple = 1.15
        return NSAttributedString(string: text + "\n", attributes: [.font: font, .foregroundColor: color, .paragraphStyle: style])
    }
}
