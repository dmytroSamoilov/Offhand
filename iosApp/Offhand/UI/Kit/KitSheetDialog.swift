import SwiftUI

struct OffhandSheet<TitleTrailing: View, Content: View>: View {
    var title: String? = nil
    var fitsContent = true
    @ViewBuilder let titleTrailing: () -> TitleTrailing
    @ViewBuilder let content: () -> Content
    @State private var contentHeight: CGFloat = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            SheetGrabber()
            if let title {
                HStack(spacing: OffhandSpacing.controls) {
                    SheetHeading(title).frame(maxWidth: .infinity, alignment: .leading)
                    titleTrailing()
                }
                .padding(.top, 8)
                .padding(.bottom, 16)
            }
            content()
        }
        .padding(.horizontal, OffhandSpacing.screen)
        .padding(.bottom, 28)
        .foregroundStyle(OffhandColor.ink)
        .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { contentHeight = $0 }
        .frame(maxHeight: fitsContent ? nil : .infinity, alignment: .top)
        .presentationDetents(detents)
        .presentationDragIndicator(.hidden)
        .presentationCornerRadius(OffhandRadius.sheet)
        .presentationBackground(OffhandColor.surface)
    }

    private var detents: Set<PresentationDetent> {
        fitsContent && contentHeight > 0 ? [.height(contentHeight)] : [.large]
    }
}

extension OffhandSheet where TitleTrailing == EmptyView {
    init(title: String? = nil, fitsContent: Bool = true, @ViewBuilder content: @escaping () -> Content) {
        self.init(title: title, fitsContent: fitsContent, titleTrailing: { EmptyView() }, content: content)
    }
}

struct SheetHeading: View {
    private let text: String

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text)
            .offhandText(.heading)
            .foregroundStyle(OffhandColor.ink)
            .accessibilityAddTraits(.isHeader)
    }
}

private struct SheetGrabber: View {
    var body: some View {
        Capsule()
            .fill(OffhandColor.border)
            .frame(width: 36, height: 4)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity)
            .accessibilityHidden(true)
    }
}

struct OffhandDialog<Content: View>: View {
    let title: String
    var message: String? = nil
    var confirmLabel: String? = nil
    var onConfirm: () -> Void = {}
    var isConfirmEnabled = true
    var destructiveLabel: String? = nil
    var onDestructive: () -> Void = {}
    var dismissLabel: String? = nil
    var onDismiss: () -> Void = {}
    @ViewBuilder let content: () -> Content
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: OffhandRadius.sheet, style: .continuous)
        VStack(alignment: .leading, spacing: 0) {
            SheetHeading(title)
            if let message {
                Text(message)
                    .offhandText(.dialogBody)
                    .foregroundStyle(OffhandColor.ink2)
                    .padding(.top, 10)
            }
            content()
            actions.padding(.top, 24)
        }
        .padding(.top, 28)
        .padding(.horizontal, OffhandSpacing.screen)
        .padding(.bottom, 16)
        .frame(maxWidth: 420)
        .background(OffhandColor.surface, in: shape)
        .overlay { shape.strokeBorder(colorScheme == .dark ? OffhandColor.border : .clear, lineWidth: 1) }
        .accessibilityElement(children: .contain)
        .accessibilityAddTraits(.isModal)
        .accessibilityAction(.escape, onDismiss)
    }

    private var actions: some View {
        VStack(spacing: 4) {
            if let confirmLabel {
                PrimaryButton(confirmLabel, fillsWidth: true, action: onConfirm)
                    .disabled(!isConfirmEnabled)
            }
            if let destructiveLabel {
                PlainTextButton(destructiveLabel, tone: .danger, fillsWidth: true, action: onDestructive)
            }
            if let dismissLabel {
                PlainTextButton(dismissLabel, tone: .muted, fillsWidth: true, action: onDismiss)
            }
        }
    }
}

extension OffhandDialog where Content == EmptyView {
    init(
        title: String,
        message: String? = nil,
        confirmLabel: String? = nil,
        onConfirm: @escaping () -> Void = {},
        isConfirmEnabled: Bool = true,
        destructiveLabel: String? = nil,
        onDestructive: @escaping () -> Void = {},
        dismissLabel: String? = nil,
        onDismiss: @escaping () -> Void = {}
    ) {
        self.init(
            title: title,
            message: message,
            confirmLabel: confirmLabel,
            onConfirm: onConfirm,
            isConfirmEnabled: isConfirmEnabled,
            destructiveLabel: destructiveLabel,
            onDestructive: onDestructive,
            dismissLabel: dismissLabel,
            onDismiss: onDismiss,
            content: { EmptyView() }
        )
    }
}

extension View {
    func offhandDialog<Dialog: View>(
        isPresented: Binding<Bool>,
        @ViewBuilder dialog: @escaping () -> Dialog
    ) -> some View {
        modifier(OffhandDialogPresenter(isPresented: isPresented, dialog: dialog))
    }
}

private struct OffhandDialogPresenter<Dialog: View>: ViewModifier {
    @Binding var isPresented: Bool
    let dialog: () -> Dialog
    @State private var isCoverPresented = false

    func body(content: Content) -> some View {
        content
            .onChange(of: isPresented, initial: true) { _, presented in
                var transaction = Transaction()
                transaction.disablesAnimations = true
                withTransaction(transaction) { isCoverPresented = presented }
            }
            .fullScreenCover(isPresented: $isCoverPresented) {
                OffhandDialogBackdrop(onDismiss: { isPresented = false }, dialog: dialog)
                    .presentationBackground(.clear)
            }
    }
}

private struct OffhandDialogBackdrop<Dialog: View>: View {
    let onDismiss: () -> Void
    @ViewBuilder let dialog: () -> Dialog
    @State private var isShown = false

    var body: some View {
        ZStack {
            OffhandColor.scrim
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)
                .accessibilityHidden(true)
            dialog().padding(.horizontal, 28)
        }
        .opacity(isShown ? 1 : 0)
        .onAppear {
            withAnimation(OffhandMotion.state) { isShown = true }
        }
    }
}
