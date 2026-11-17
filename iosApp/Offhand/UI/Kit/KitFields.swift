import SwiftUI

struct SearchField: View {
    @Binding var query: String
    let placeholder: String
    let clearLabel: String
    var onSubmit: () -> Void = {}
    @FocusState private var isFocused: Bool

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 18, weight: .regular))
                .foregroundStyle(OffhandColor.ink2)
                .accessibilityHidden(true)
            TextField(placeholder, text: $query, prompt: Text(placeholder).foregroundStyle(OffhandColor.ink3))
                .offhandText(.body)
                .foregroundStyle(OffhandColor.ink)
                .tint(OffhandColor.ink)
                .focused($isFocused)
                .submitLabel(.search)
                .onSubmit(onSubmit)
            if !query.isEmpty {
                BarIconButton(systemImage: "xmark", label: clearLabel, tint: OffhandColor.ink2) { query = "" }
            }
        }
        .padding(.leading, 16)
        .padding(.trailing, 6)
        .frame(minHeight: 48)
        .background(OffhandColor.surface, in: Capsule())
        .overlay {
            Capsule().strokeBorder(isFocused ? OffhandColor.ink : OffhandColor.border, lineWidth: isFocused ? 1.5 : 1)
        }
        .contentShape(Capsule())
        .onTapGesture { isFocused = true }
        .animation(OffhandMotion.state, value: isFocused)
    }
}

struct OffhandTextField<Trailing: View>: View {
    @Binding var text: String
    var label: String? = nil
    var placeholder: String? = nil
    var error: String? = nil
    var isMultiline = false
    var minLines = 1
    var isSecure = false
    var isFilledWithGround = false
    @ViewBuilder let trailing: () -> Trailing
    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if let label {
                Text(label)
                    .offhandText(.caption, face: .interSemibold)
                    .foregroundStyle(OffhandColor.ink)
            }
            box
            if let error {
                Text(error)
                    .offhandText(.caption)
                    .foregroundStyle(OffhandColor.danger)
            }
        }
        .animation(OffhandMotion.state, value: isFocused)
    }

    private var box: some View {
        let shape = RoundedRectangle(cornerRadius: OffhandRadius.field, style: .continuous)
        return HStack(alignment: isMultiline ? .top : .center, spacing: 0) {
            field
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.trailing, 12)
            trailing()
        }
        .padding(.leading, 16)
        .padding(.trailing, 4)
        .padding(.vertical, 12)
        .frame(minHeight: 52)
        .background(isFilledWithGround ? OffhandColor.ground : OffhandColor.surface, in: shape)
        .overlay { shape.strokeBorder(borderColor, lineWidth: isFocused || error != nil ? 1.5 : 1) }
        .contentShape(shape)
        .onTapGesture { isFocused = true }
    }

    private var field: some View {
        Group {
            if isSecure {
                SecureField(title, text: $text, prompt: prompt)
            } else if isMultiline {
                TextField(title, text: $text, prompt: prompt, axis: .vertical)
                    .lineLimit(minLines...)
            } else {
                TextField(title, text: $text, prompt: prompt)
            }
        }
        .offhandText(.body)
        .foregroundStyle(OffhandColor.ink)
        .tint(OffhandColor.ink)
        .focused($isFocused)
    }

    private var title: String { label ?? placeholder ?? "" }

    private var prompt: Text? {
        placeholder.map { Text($0).foregroundStyle(OffhandColor.ink3) }
    }

    private var borderColor: Color {
        if error != nil { return OffhandColor.danger }
        return isFocused ? OffhandColor.ink : OffhandColor.border
    }
}

extension OffhandTextField where Trailing == EmptyView {
    init(
        text: Binding<String>,
        label: String? = nil,
        placeholder: String? = nil,
        error: String? = nil,
        isMultiline: Bool = false,
        minLines: Int = 1,
        isSecure: Bool = false,
        isFilledWithGround: Bool = false
    ) {
        self.init(
            text: text,
            label: label,
            placeholder: placeholder,
            error: error,
            isMultiline: isMultiline,
            minLines: minLines,
            isSecure: isSecure,
            isFilledWithGround: isFilledWithGround,
            trailing: { EmptyView() }
        )
    }
}
