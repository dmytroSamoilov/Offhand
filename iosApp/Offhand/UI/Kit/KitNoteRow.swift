import SwiftUI

enum NoteRowState {
    case ready(preview: AttributedString?)
    case processing(text: String, progress: Double?)
    case attention(text: String)
}

struct NoteRow<Leading: View, Trailing: View>: View {
    let title: AttributedString
    let state: NoteRowState
    let meta: String?
    let waveSeed: Int64
    var showsWave = true
    var previewLines = 1
    @ViewBuilder let leading: () -> Leading
    @ViewBuilder let trailing: () -> Trailing

    var body: some View {
        HStack(spacing: 16) {
            leading()
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .offhandText(.rowTitle)
                    .foregroundStyle(OffhandColor.ink)
                    .lineLimit(2)
                NoteRowBody(state: state, previewLines: previewLines)
                if let meta {
                    MetaText(meta).padding(.top, 2)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if showsGlyph {
                WaveGlyph(seed: waveSeed)
            }
            trailing()
        }
        .padding(.top, 14)
        .padding(.bottom, 16)
    }

    private var showsGlyph: Bool {
        guard showsWave, case .ready = state else { return false }
        return true
    }
}

extension NoteRow where Leading == EmptyView, Trailing == EmptyView {
    init(title: AttributedString, state: NoteRowState, meta: String?, waveSeed: Int64, showsWave: Bool = true) {
        self.init(
            title: title,
            state: state,
            meta: meta,
            waveSeed: waveSeed,
            showsWave: showsWave,
            leading: { EmptyView() },
            trailing: { EmptyView() }
        )
    }
}

extension NoteRow where Trailing == EmptyView {
    init(
        title: AttributedString,
        state: NoteRowState,
        meta: String?,
        waveSeed: Int64,
        showsWave: Bool = true,
        previewLines: Int = 1,
        @ViewBuilder leading: @escaping () -> Leading
    ) {
        self.init(
            title: title,
            state: state,
            meta: meta,
            waveSeed: waveSeed,
            showsWave: showsWave,
            previewLines: previewLines,
            leading: leading,
            trailing: { EmptyView() }
        )
    }
}

private struct NoteRowBody: View {
    let state: NoteRowState
    let previewLines: Int

    var body: some View {
        switch state {
        case .ready(let preview):
            if let preview {
                Text(preview)
                    .offhandText(.bodySmall)
                    .foregroundStyle(OffhandColor.ink2)
                    .lineLimit(previewLines)
            }
        case .processing(let text, let progress):
            Text(text)
                .offhandText(.bodySmall, face: .interMedium)
                .foregroundStyle(OffhandColor.accent)
            ProgressLine(progress: progress)
                .padding(.top, 6)
                .padding(.bottom, 2)
        case .attention(let text):
            Text(text)
                .offhandText(.bodySmall, face: .interMedium)
                .foregroundStyle(OffhandColor.ink2)
        }
    }
}

struct WaveGlyph: View {
    let seed: Int64
    var color: Color = OffhandColor.wave
    var size = CGSize(width: 44, height: 24)
    var barCount = 9
    var lineWidth: CGFloat = 2

    var body: some View {
        let bars = waveBars(seed: seed, count: barCount)
        Canvas { context, canvasSize in
            context.stroke(
                waveBarPath(bars: bars, size: canvasSize, lineWidth: lineWidth) { _ in true },
                with: .color(color),
                style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
            )
        }
        .frame(width: size.width, height: size.height)
        .accessibilityHidden(true)
    }
}

func waveBars(seed: Int64, count: Int) -> [CGFloat] {
    var generator = SeededWaveGenerator(seed: UInt64(bitPattern: seed))
    return (0..<count).map { _ in CGFloat.random(in: WaveShape.minBar...1, using: &generator) }
}

func waveBarPath(bars: [CGFloat], size: CGSize, lineWidth: CGFloat, includes: (Int) -> Bool) -> Path {
    let step = size.width / CGFloat(max(bars.count, 1))
    let middle = size.height / 2
    return Path { path in
        for (index, height) in bars.enumerated() where includes(index) {
            let x = step * CGFloat(index) + step / 2
            let half = (size.height - lineWidth) * height / 2
            path.move(to: CGPoint(x: x, y: middle - half))
            path.addLine(to: CGPoint(x: x, y: middle + half))
        }
    }
}

private enum WaveShape {
    static let minBar: CGFloat = 0.18
}

private struct SeededWaveGenerator: RandomNumberGenerator {
    var state: UInt64

    init(seed: UInt64) {
        state = seed
    }

    mutating func next() -> UInt64 {
        state &+= 0x9E37_79B9_7F4A_7C15
        var mixed = state
        mixed = (mixed ^ (mixed >> 30)) &* 0xBF58_476D_1CE4_E5B9
        mixed = (mixed ^ (mixed >> 27)) &* 0x94D0_49BB_1331_11EB
        return mixed ^ (mixed >> 31)
    }
}
