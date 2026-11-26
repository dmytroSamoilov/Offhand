import OffhandShared
import SwiftUI

struct UsageSurveyCard: View {
    let stage: UsageSurveyStageUi
    let onAnswer: () -> Void
    let onNotNow: () -> Void
    let onThanksClosed: () -> Void

    var body: some View {
        switch stage {
        case .question: questionCard
        case .thanks: thanksCard
        }
    }

    private var questionCard: some View {
        SurfaceCard {
            VStack(alignment: .leading, spacing: 0) {
                UsageSurveyCardText(
                    title: String(localized: "What do you mostly record?"),
                    message: String(localized: "I build Offhand on my own. Your anonymous answer helps me make it better for you.")
                )
                .contentShape(Rectangle())
                .onTapGesture(perform: onAnswer)
                HStack(spacing: 4) {
                    PlainTextButton(String(localized: "Answer"), action: onAnswer)
                    PlainTextButton(String(localized: "Not now"), tone: .muted, action: onNotNow)
                }
                .padding(.top, 4)
                .padding(.leading, -OffhandSpacing.controls)
            }
            .padding(.horizontal, 18)
            .padding(.top, 16)
            .padding(.bottom, 6)
        }
    }

    private var thanksCard: some View {
        SurfaceCard {
            HStack(alignment: .center, spacing: 8) {
                UsageSurveyCardText(
                    title: String(localized: "Thanks a lot!"),
                    message: String(localized: "You made my day a bit better. Now I’ll make Offhand better for you.")
                )
                CountdownCloseButton(onClosed: onThanksClosed)
            }
            .padding(.leading, 18)
            .padding(.trailing, 10)
            .padding(.vertical, 16)
        }
    }
}

private struct UsageSurveyCardText: View {
    let title: String
    let message: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .offhandText(.rowTitle)
                .foregroundStyle(OffhandColor.ink)
                .accessibilityAddTraits(.isHeader)
            Text(message)
                .offhandText(.bodySmall)
                .foregroundStyle(OffhandColor.ink2)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct CountdownCloseButton: View {
    let onClosed: () -> Void
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var remaining: CGFloat = 1

    private static let visibleSeconds: Double = 10
    private static let ringSize: CGFloat = 32
    private static let ringWidth: CGFloat = 2.5

    var body: some View {
        Button(action: onClosed) {
            ZStack {
                Circle().stroke(OffhandColor.hairline, lineWidth: Self.ringWidth)
                Circle()
                    .trim(from: 0, to: remaining)
                    .stroke(OffhandColor.accent, style: StrokeStyle(lineWidth: Self.ringWidth, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                Image(systemName: "xmark")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(OffhandColor.ink2)
            }
            .frame(width: Self.ringSize, height: Self.ringSize)
            .frame(width: OffhandSpacing.touchTarget, height: OffhandSpacing.touchTarget)
            .contentShape(Circle())
        }
        .buttonStyle(.offhandPress)
        .accessibilityLabel(String(localized: "Close"))
        .task { await closeWhenTimeIsUp() }
    }

    private func closeWhenTimeIsUp() async {
        remaining = 1
        if !reduceMotion {
            withAnimation(.linear(duration: Self.visibleSeconds)) { remaining = 0 }
        }
        try? await Task.sleep(for: .seconds(Self.visibleSeconds))
        if !Task.isCancelled { onClosed() }
    }
}

struct UsageSurveySheet: View {
    let selection: Set<UsageSurveyAnswerUi>
    let onToggled: (UsageSurveyAnswerUi) -> Void
    let onClosed: () -> Void

    var body: some View {
        OffhandSheet(title: String(localized: "What do you mostly record?")) {
            Text(String(localized: "I build Offhand on my own. Your anonymous answer helps me make it better for you."))
                .offhandText(.dialogBody)
                .foregroundStyle(OffhandColor.ink2)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.bottom, 4)
            VStack(spacing: 0) {
                ForEach(UsageSurveyAnswerUi.allCases, id: \.self) { answer in
                    ChoiceRow(
                        title: answer.label,
                        isSelected: selection.contains(answer),
                        showsDivider: answer != UsageSurveyAnswerUi.allCases.last
                    ) {
                        onToggled(answer)
                    }
                }
            }
            PrimaryButton(String(localized: "Confirm"), fillsWidth: true, action: onClosed)
                .disabled(selection.isEmpty)
                .padding(.top, 18)
        }
    }
}

private extension UsageSurveyAnswerUi {
    var label: String {
        switch self {
        case .meetings: String(localized: "Meetings")
        case .thoughts: String(localized: "My own thoughts and ideas")
        case .lectures: String(localized: "Lectures and classes")
        case .interviews: String(localized: "Interviews and calls")
        case .other: String(localized: "Something else")
        }
    }
}
