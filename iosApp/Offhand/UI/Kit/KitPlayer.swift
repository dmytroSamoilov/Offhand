import SwiftUI

struct Player: View {
    let isPlaying: Bool
    let progress: Double
    let elapsedLabel: String
    let playLabel: String
    let pauseLabel: String
    let seekLabel: String
    let waveSeed: Int64
    let onToggle: () -> Void
    let onSeek: (Double) -> Void

    var body: some View {
        HStack(spacing: 14) {
            RoundIconButton(
                systemImage: isPlaying ? "pause.fill" : "play.fill",
                label: isPlaying ? pauseLabel : playLabel,
                size: 48,
                isFilled: true,
                action: onToggle
            )
            PlayerWaveform(progress: progress, waveSeed: waveSeed, seekLabel: seekLabel, onSeek: onSeek)
            Text(elapsedLabel)
                .font(.offhandMonoSmall)
                .foregroundStyle(OffhandColor.ink2)
                .lineLimit(1)
        }
    }
}

private struct PlayerWaveform: View {
    let progress: Double
    let waveSeed: Int64
    let seekLabel: String
    let onSeek: (Double) -> Void
    @State private var width: CGFloat = 0

    private static let barCount = 37
    private static let lineWidth: CGFloat = 2.5
    private static let accessibilityStep = 0.05

    var body: some View {
        let bars = waveBars(seed: waveSeed, count: Self.barCount)
        Canvas { context, size in
            draw(bars: bars, in: &context, size: size)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 32)
        .contentShape(Rectangle())
        .onGeometryChange(for: CGFloat.self) { $0.size.width } action: { width = $0 }
        .gesture(DragGesture(minimumDistance: 0).onChanged { seek(to: $0.location.x) })
        .accessibilityElement()
        .accessibilityLabel(seekLabel)
        .accessibilityValue(progress.formatted(.percent.precision(.fractionLength(0))))
        .accessibilityAdjustableAction(adjust)
    }

    private func draw(bars: [CGFloat], in context: inout GraphicsContext, size: CGSize) {
        let style = StrokeStyle(lineWidth: Self.lineWidth, lineCap: .round)
        let step = size.width / CGFloat(bars.count)
        let isPlayed: (Int) -> Bool = { (step * CGFloat($0) + step / 2) / max(size.width, 1) <= progress }
        let played = waveBarPath(bars: bars, size: size, lineWidth: Self.lineWidth, includes: isPlayed)
        let rest = waveBarPath(bars: bars, size: size, lineWidth: Self.lineWidth) { !isPlayed($0) }
        context.stroke(played, with: .color(OffhandColor.ink), style: style)
        context.stroke(rest, with: .color(OffhandColor.wave), style: style)
    }

    private func seek(to x: CGFloat) {
        guard width > 0 else { return }
        onSeek(Double(min(max(x / width, 0), 1)))
    }

    private func adjust(_ direction: AccessibilityAdjustmentDirection) {
        switch direction {
        case .increment: onSeek(min(progress + Self.accessibilityStep, 1))
        case .decrement: onSeek(max(progress - Self.accessibilityStep, 0))
        @unknown default: break
        }
    }
}
