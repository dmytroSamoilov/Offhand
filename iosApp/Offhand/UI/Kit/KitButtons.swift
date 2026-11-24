import SwiftUI

enum KitAlpha {
    static let disabled: Double = 0.4
    static let pressed: Double = 0.75
}

enum TextButtonTone {
    case accent, ink, muted, danger

    var color: Color {
        switch self {
        case .accent: OffhandColor.accent
        case .ink: OffhandColor.ink
        case .muted: OffhandColor.ink2
        case .danger: OffhandColor.danger
        }
    }
}

enum OffhandPillKind {
    case primary, secondary, gold

    var container: Color {
        switch self {
        case .primary: OffhandColor.ink
        case .secondary: OffhandColor.surface
        case .gold: OffhandColor.gold
        }
    }

    var content: Color {
        switch self {
        case .primary: OffhandColor.onInk
        case .secondary: OffhandColor.ink
        case .gold: OffhandColor.onGold
        }
    }

    var border: Color? { self == .secondary ? OffhandColor.border : nil }
}

struct OffhandPillButtonStyle: ButtonStyle {
    let kind: OffhandPillKind
    var fillsWidth = false
    var height: CGFloat = OffhandSpacing.control

    func makeBody(configuration: Configuration) -> some View {
        PillButtonBody(configuration: configuration, kind: kind, fillsWidth: fillsWidth, height: height)
    }
}

private struct PillButtonBody: View {
    let configuration: ButtonStyleConfiguration
    let kind: OffhandPillKind
    let fillsWidth: Bool
    let height: CGFloat
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        configuration.label
            .offhandText(.button)
            .lineLimit(1)
            .foregroundStyle(kind.content)
            .padding(.horizontal, OffhandSpacing.screen)
            .frame(maxWidth: fillsWidth ? .infinity : nil, minHeight: height)
            .background(kind.container, in: Capsule())
            .overlay {
                if let border = kind.border { Capsule().strokeBorder(border, lineWidth: 1) }
            }
            .contentShape(Capsule())
            .opacity(isEnabled ? (configuration.isPressed ? KitAlpha.pressed : 1) : KitAlpha.disabled)
    }
}

struct OffhandPlainTextButtonStyle: ButtonStyle {
    var tone: TextButtonTone = .accent
    var fillsWidth = false

    func makeBody(configuration: Configuration) -> some View {
        PlainTextButtonBody(configuration: configuration, tone: tone, fillsWidth: fillsWidth)
    }
}

private struct PlainTextButtonBody: View {
    let configuration: ButtonStyleConfiguration
    let tone: TextButtonTone
    let fillsWidth: Bool
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        configuration.label
            .offhandText(.buttonSmall)
            .foregroundStyle(tone.color)
            .padding(.horizontal, OffhandSpacing.controls)
            .frame(maxWidth: fillsWidth ? .infinity : nil, minHeight: OffhandSpacing.touchTarget)
            .contentShape(Capsule())
            .opacity(isEnabled ? (configuration.isPressed ? KitAlpha.pressed : 1) : KitAlpha.disabled)
    }
}

struct OffhandPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        PressBody(configuration: configuration)
    }
}

private struct PressBody: View {
    let configuration: ButtonStyleConfiguration
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        configuration.label
            .opacity(isEnabled ? (configuration.isPressed ? KitAlpha.pressed : 1) : KitAlpha.disabled)
    }
}

struct OffhandRowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        RowButtonBody(configuration: configuration)
    }
}

private struct RowButtonBody: View {
    let configuration: ButtonStyleConfiguration
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        configuration.label
            .contentShape(Rectangle())
            .background(configuration.isPressed ? OffhandColor.well : .clear)
            .opacity(isEnabled ? 1 : KitAlpha.disabled)
    }
}

extension ButtonStyle where Self == OffhandPillButtonStyle {
    static var offhandPrimary: OffhandPillButtonStyle { OffhandPillButtonStyle(kind: .primary) }
    static var offhandSecondary: OffhandPillButtonStyle { OffhandPillButtonStyle(kind: .secondary) }
    static var offhandGold: OffhandPillButtonStyle { OffhandPillButtonStyle(kind: .gold) }

    static func offhandPrimary(fillsWidth: Bool) -> OffhandPillButtonStyle {
        OffhandPillButtonStyle(kind: .primary, fillsWidth: fillsWidth)
    }

    static func offhandSecondary(fillsWidth: Bool) -> OffhandPillButtonStyle {
        OffhandPillButtonStyle(kind: .secondary, fillsWidth: fillsWidth)
    }
}

extension ButtonStyle where Self == OffhandPlainTextButtonStyle {
    static func offhandPlainText(_ tone: TextButtonTone = .accent) -> OffhandPlainTextButtonStyle {
        OffhandPlainTextButtonStyle(tone: tone)
    }
}

extension ButtonStyle where Self == OffhandPressStyle {
    static var offhandPress: OffhandPressStyle { OffhandPressStyle() }
}

extension ButtonStyle where Self == OffhandRowButtonStyle {
    static var offhandRow: OffhandRowButtonStyle { OffhandRowButtonStyle() }
}

struct OffhandButtonLabel: View {
    let text: String
    var systemImage: String? = nil

    var body: some View {
        HStack(spacing: 10) {
            if let systemImage {
                Image(systemName: systemImage)
                    .font(.system(size: 18, weight: .regular))
                    .accessibilityHidden(true)
            }
            Text(text)
        }
    }
}

struct PrimaryButton: View {
    private let text: String
    private let systemImage: String?
    private let style: OffhandPillButtonStyle
    private let action: () -> Void

    init(
        _ text: String,
        systemImage: String? = nil,
        fillsWidth: Bool = false,
        height: CGFloat = OffhandSpacing.control,
        action: @escaping () -> Void
    ) {
        self.text = text
        self.systemImage = systemImage
        self.style = OffhandPillButtonStyle(kind: .primary, fillsWidth: fillsWidth, height: height)
        self.action = action
    }

    var body: some View {
        Button(action: action) { OffhandButtonLabel(text: text, systemImage: systemImage) }
            .buttonStyle(style)
    }
}

struct SecondaryButton: View {
    private let text: String
    private let systemImage: String?
    private let style: OffhandPillButtonStyle
    private let action: () -> Void

    init(
        _ text: String,
        systemImage: String? = nil,
        fillsWidth: Bool = false,
        height: CGFloat = OffhandSpacing.control,
        action: @escaping () -> Void
    ) {
        self.text = text
        self.systemImage = systemImage
        self.style = OffhandPillButtonStyle(kind: .secondary, fillsWidth: fillsWidth, height: height)
        self.action = action
    }

    var body: some View {
        Button(action: action) { OffhandButtonLabel(text: text, systemImage: systemImage) }
            .buttonStyle(style)
    }
}

struct GoldButton: View {
    private let text: String
    private let fillsWidth: Bool
    private let action: () -> Void

    init(_ text: String, fillsWidth: Bool = false, action: @escaping () -> Void) {
        self.text = text
        self.fillsWidth = fillsWidth
        self.action = action
    }

    var body: some View {
        Button(text, action: action)
            .buttonStyle(OffhandPillButtonStyle(kind: .gold, fillsWidth: fillsWidth))
    }
}

struct PlainTextButton: View {
    private let text: String
    private let tone: TextButtonTone
    private let systemImage: String?
    private let fillsWidth: Bool
    private let action: () -> Void

    init(
        _ text: String,
        tone: TextButtonTone = .accent,
        systemImage: String? = nil,
        fillsWidth: Bool = false,
        action: @escaping () -> Void
    ) {
        self.text = text
        self.tone = tone
        self.systemImage = systemImage
        self.fillsWidth = fillsWidth
        self.action = action
    }

    var body: some View {
        Button(action: action) { OffhandButtonLabel(text: text, systemImage: systemImage) }
            .buttonStyle(OffhandPlainTextButtonStyle(tone: tone, fillsWidth: fillsWidth))
    }
}

struct RecordPill: View {
    private let text: String
    private let accessibilityText: String?
    private let height: CGFloat
    private let fillsWidth: Bool
    private let action: () -> Void
    @Environment(\.colorScheme) private var colorScheme

    init(
        _ text: String,
        accessibilityLabel: String? = nil,
        height: CGFloat = OffhandSpacing.control,
        fillsWidth: Bool = false,
        action: @escaping () -> Void
    ) {
        self.text = text
        self.accessibilityText = accessibilityLabel
        self.height = height
        self.fillsWidth = fillsWidth
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Circle()
                    .fill(OffhandColor.record)
                    .frame(width: 10, height: 10)
                Text(text)
                    .lineLimit(1)
                    .fixedSize()
            }
            .padding(.trailing, 4)
        }
        .buttonStyle(OffhandPillButtonStyle(kind: .primary, fillsWidth: fillsWidth, height: height))
        .shadow(color: colorScheme == .dark ? .clear : OffhandColor.ink.opacity(0.2), radius: 12, x: 0, y: 10)
        .accessibilityLabel(accessibilityText ?? text)
    }
}

struct PausePill: View {
    private let text: String
    private let isPaused: Bool
    private let action: () -> Void

    init(_ text: String, isPaused: Bool, action: @escaping () -> Void) {
        self.text = text
        self.isPaused = isPaused
        self.action = action
    }

    var body: some View {
        SecondaryButton(text, systemImage: isPaused ? "play.fill" : "pause.fill", action: action)
    }
}

struct RoundIconButton: View {
    private let systemImage: String
    private let label: String
    private let size: CGFloat
    private let isFilled: Bool
    private let action: () -> Void

    init(
        systemImage: String,
        label: String,
        size: CGFloat = OffhandSpacing.control,
        isFilled: Bool = false,
        action: @escaping () -> Void
    ) {
        self.systemImage = systemImage
        self.label = label
        self.size = size
        self.isFilled = isFilled
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: size > 48 ? 20 : 18, weight: .regular))
                .foregroundStyle(isFilled ? OffhandColor.onInk : OffhandColor.ink)
                .frame(width: size, height: size)
                .background(isFilled ? OffhandColor.ink : OffhandColor.surface, in: Circle())
                .overlay {
                    if !isFilled { Circle().strokeBorder(OffhandColor.border, lineWidth: 1) }
                }
                .contentShape(Circle())
        }
        .buttonStyle(.offhandPress)
        .accessibilityLabel(label)
    }
}

struct BarIconButton: View {
    private let systemImage: String
    private let label: String
    private let tint: Color
    private let action: () -> Void

    init(systemImage: String, label: String, tint: Color = OffhandColor.ink, action: @escaping () -> Void) {
        self.systemImage = systemImage
        self.label = label
        self.tint = tint
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 22, weight: .regular))
                .foregroundStyle(tint)
                .frame(width: OffhandSpacing.touchTarget, height: OffhandSpacing.touchTarget)
                .contentShape(Rectangle())
        }
        .buttonStyle(.offhandPress)
        .accessibilityLabel(label)
    }
}
