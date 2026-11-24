import CoreText
import SwiftUI
import UIKit

enum OffhandFonts {
    private static let files = [
        "inter_regular", "inter_medium", "inter_semibold", "inter_bold",
        "inter_tight_medium", "inter_tight_semibold",
        "jetbrains_mono_regular", "jetbrains_mono_medium",
    ]
    private static var isRegistered = false

    static func register(bundle: Bundle = .main) {
        guard !isRegistered else { return }
        isRegistered = true
        let urls = files.compactMap { bundle.url(forResource: $0, withExtension: "ttf") }
        guard !urls.isEmpty else { return }
        CTFontManagerRegisterFontURLs(urls as CFArray, .process, true, nil)
    }
}

enum OffhandTextStyle {
    case display, title, heading, rowTitle, body, bodySmall, label, meta
    case button, buttonSmall, tab, tabSelected, settingTitle, caption, number, timer
    case dialogBody, emptyTitle

    var spec: OffhandTextSpec {
        switch self {
        case .display: OffhandTextSpec(face: .tightSemibold, size: 34, line: 40, tracking: -0.02, relativeTo: .largeTitle)
        case .title: OffhandTextSpec(face: .tightSemibold, size: 28, line: 34, tracking: -0.02, relativeTo: .title)
        case .heading: OffhandTextSpec(face: .tightSemibold, size: 22, line: 28, tracking: -0.01, relativeTo: .title2)
        case .rowTitle: OffhandTextSpec(face: .interSemibold, size: 17, line: 22, tracking: -0.01, relativeTo: .headline)
        case .body: OffhandTextSpec(face: .interRegular, size: 16, line: 25, tracking: 0, relativeTo: .body)
        case .bodySmall: OffhandTextSpec(face: .interRegular, size: 14, line: 21, tracking: 0, relativeTo: .subheadline)
        case .label: OffhandTextSpec(face: .interSemibold, size: 11, line: 16, tracking: 0.1, relativeTo: .caption2, isCaps: true)
        case .meta: OffhandTextSpec(face: .monoRegular, size: 11, line: 16, tracking: 0.04, relativeTo: .caption2, isCaps: true)
        case .button: OffhandTextSpec(face: .interSemibold, size: 16, line: 22, tracking: 0, relativeTo: .body)
        case .buttonSmall: OffhandTextSpec(face: .interSemibold, size: 15, line: 22, tracking: 0, relativeTo: .subheadline)
        case .tab: OffhandTextSpec(face: .interMedium, size: 15, line: 22, tracking: 0, relativeTo: .subheadline)
        case .tabSelected: OffhandTextSpec(face: .interSemibold, size: 15, line: 22, tracking: 0, relativeTo: .subheadline)
        case .settingTitle: OffhandTextSpec(face: .interMedium, size: 15, line: 21, tracking: 0, relativeTo: .subheadline)
        case .caption: OffhandTextSpec(face: .interRegular, size: 13, line: 20, tracking: 0, relativeTo: .footnote)
        case .number: OffhandTextSpec(face: .monoRegular, size: 13, line: 18, tracking: 0, relativeTo: .footnote)
        case .timer: OffhandTextSpec(face: .monoRegular, size: 68, line: 72, tracking: -0.04, relativeTo: .largeTitle)
        case .dialogBody: OffhandTextSpec(face: .interRegular, size: 15, line: 23, tracking: 0, relativeTo: .subheadline)
        case .emptyTitle: OffhandTextSpec(face: .tightSemibold, size: 24, line: 30, tracking: -0.01, relativeTo: .title2)
        }
    }
}

enum OffhandFace: String {
    case interRegular = "Inter-Regular"
    case interMedium = "Inter-Medium"
    case interSemibold = "Inter-SemiBold"
    case interBold = "Inter-Bold"
    case tightMedium = "InterTight-Medium"
    case tightSemibold = "InterTight-SemiBold"
    case monoRegular = "JetBrainsMonoRoman-Regular"
    case monoMedium = "JetBrainsMonoRoman-Medium"
}

struct OffhandTextSpec {
    let face: OffhandFace
    let size: CGFloat
    let line: CGFloat
    let tracking: CGFloat
    let relativeTo: Font.TextStyle
    var isCaps = false

    var font: Font { .custom(face.rawValue, size: size, relativeTo: relativeTo) }

    func with(face: OffhandFace) -> OffhandTextSpec {
        OffhandTextSpec(face: face, size: size, line: line, tracking: tracking, relativeTo: relativeTo, isCaps: isCaps)
    }

    var uiFont: UIFont {
        let base = UIFont(name: face.rawValue, size: size) ?? .systemFont(ofSize: size)
        return UIFontMetrics(forTextStyle: relativeTo.uiTextStyle).scaledFont(for: base)
    }
}

extension Font {
    static func offhand(_ style: OffhandTextStyle) -> Font { style.spec.font }

    static func offhand(_ face: OffhandFace, size: CGFloat, relativeTo: Font.TextStyle = .body) -> Font {
        .custom(face.rawValue, size: size, relativeTo: relativeTo)
    }
}

private struct OffhandTextModifier: ViewModifier {
    let spec: OffhandTextSpec

    func body(content: Content) -> some View {
        content
            .font(spec.font)
            .tracking(spec.tracking * spec.size)
            .lineSpacing(max(0, spec.line - spec.uiFont.lineHeight * spec.size / spec.uiFont.pointSize))
            .textCase(spec.isCaps ? .uppercase : nil)
    }
}

extension View {
    func offhandText(_ style: OffhandTextStyle) -> some View {
        modifier(OffhandTextModifier(spec: style.spec))
    }

    func offhandText(_ style: OffhandTextStyle, face: OffhandFace) -> some View {
        modifier(OffhandTextModifier(spec: style.spec.with(face: face)))
    }
}

private extension Font.TextStyle {
    var uiTextStyle: UIFont.TextStyle {
        switch self {
        case .largeTitle: .largeTitle
        case .title: .title1
        case .title2: .title2
        case .title3: .title3
        case .headline: .headline
        case .subheadline: .subheadline
        case .callout: .callout
        case .footnote: .footnote
        case .caption: .caption1
        case .caption2: .caption2
        default: .body
        }
    }
}
