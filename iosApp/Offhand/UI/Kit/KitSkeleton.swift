import SwiftUI

struct Skeleton: View {
    var lineWidths: [CGFloat] = [0.92, 1, 0.64]
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    static let lineHeight: CGFloat = 10
    static let lineSpacing: CGFloat = 10

    var body: some View {
        Group {
            if reduceMotion {
                SkeletonCanvas(lineWidths: lineWidths, phase: nil)
            } else {
                TimelineView(.animation) { timeline in
                    SkeletonCanvas(lineWidths: lineWidths, phase: phase(at: timeline.date))
                }
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: height)
        .accessibilityHidden(true)
    }

    private var height: CGFloat {
        let count = CGFloat(lineWidths.count)
        return count * Self.lineHeight + max(count - 1, 0) * Self.lineSpacing
    }

    private func phase(at date: Date) -> Double {
        date.timeIntervalSinceReferenceDate
            .truncatingRemainder(dividingBy: OffhandMotion.skeletonSweep) / OffhandMotion.skeletonSweep
    }
}

private struct SkeletonCanvas: View {
    let lineWidths: [CGFloat]
    let phase: Double?

    private static let bandShare: CGFloat = 0.6

    var body: some View {
        Canvas { context, size in
            let shading = shading(width: size.width)
            for (index, fraction) in lineWidths.enumerated() {
                let rect = CGRect(
                    x: 0,
                    y: CGFloat(index) * (Skeleton.lineHeight + Skeleton.lineSpacing),
                    width: size.width * fraction,
                    height: Skeleton.lineHeight
                )
                context.fill(Capsule().path(in: rect), with: shading)
            }
        }
    }

    private func shading(width: CGFloat) -> GraphicsContext.Shading {
        guard let phase else { return .color(OffhandColor.hairline) }
        let band = width * Self.bandShare
        let start = -band + CGFloat(phase) * (width + band)
        return .linearGradient(
            Gradient(colors: [OffhandColor.hairline, OffhandColor.well, OffhandColor.hairline]),
            startPoint: CGPoint(x: start, y: 0),
            endPoint: CGPoint(x: start + band, y: 0)
        )
    }
}
