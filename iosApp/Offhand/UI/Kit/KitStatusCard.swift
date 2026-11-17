import SwiftUI

struct StatusCard<TitleTrailing: View, Actions: View>: View {
    let title: String
    var number: String? = nil
    var progress: Double? = nil
    var showsProgress = false
    var message: String? = nil
    @ViewBuilder let titleTrailing: () -> TitleTrailing
    @ViewBuilder let actions: () -> Actions

    var body: some View {
        SurfaceCard {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .firstTextBaseline, spacing: 12) {
                    Text(title)
                        .offhandText(.buttonSmall)
                        .foregroundStyle(OffhandColor.ink)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if let number {
                        Text(number)
                            .offhandText(.number)
                            .foregroundStyle(OffhandColor.ink2)
                    }
                    titleTrailing()
                }
                if showsProgress {
                    ProgressLine(progress: progress, thickness: 3)
                }
                if let message {
                    Text(message)
                        .offhandText(.caption)
                        .foregroundStyle(OffhandColor.ink2)
                }
                actions()
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 16)
        }
    }
}

extension StatusCard where TitleTrailing == EmptyView, Actions == EmptyView {
    init(title: String, number: String? = nil, progress: Double? = nil, showsProgress: Bool = false, message: String? = nil) {
        self.init(
            title: title,
            number: number,
            progress: progress,
            showsProgress: showsProgress,
            message: message,
            titleTrailing: { EmptyView() },
            actions: { EmptyView() }
        )
    }
}

extension StatusCard where TitleTrailing == EmptyView {
    init(
        title: String,
        number: String? = nil,
        progress: Double? = nil,
        showsProgress: Bool = false,
        message: String? = nil,
        @ViewBuilder actions: @escaping () -> Actions
    ) {
        self.init(
            title: title,
            number: number,
            progress: progress,
            showsProgress: showsProgress,
            message: message,
            titleTrailing: { EmptyView() },
            actions: actions
        )
    }
}

struct MessageCard<Actions: View>: View {
    let title: String
    let message: String
    @ViewBuilder let actions: () -> Actions

    var body: some View {
        SurfaceCard {
            VStack(alignment: .leading, spacing: 6) {
                Text(title)
                    .offhandText(.body, face: .interSemibold)
                    .foregroundStyle(OffhandColor.ink)
                Text(message)
                    .offhandText(.bodySmall)
                    .foregroundStyle(OffhandColor.ink2)
                actions()
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(18)
        }
    }
}

extension MessageCard where Actions == EmptyView {
    init(title: String, message: String) {
        self.init(title: title, message: message, actions: { EmptyView() })
    }
}
