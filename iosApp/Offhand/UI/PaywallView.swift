import OffhandShared
import SwiftUI

struct PaywallView: View {
    private let viewModel = AppViewModels.paywall
    @State private var state = PaywallUiState(
        feature: .general,
        offers: [],
        selectedPlan: .yearly,
        isLoadingOffers: true,
        isPurchasing: false,
        isPro: false,
        message: nil
    )

    private static let successDismissDelay: Duration = .milliseconds(1600)

    var body: some View {
        Group {
            if state.isPro {
                successContent
            } else {
                paywallContent
            }
        }
        .background(Color(.systemGroupedBackground))
        .alert(String(localized: "Offhand Pro"), isPresented: messageBinding) {
            Button(String(localized: "OK")) { viewModel.onMessageDismissed() }
        } message: {
            if let message = state.message { Text(messageText(message)) }
        }
        .onAppear { viewModel.onOpened() }
        .onChange(of: state.isPro) {
            guard state.isPro else { return }
            Task {
                try? await Task.sleep(for: Self.successDismissDelay)
                viewModel.onClosed()
            }
        }
        .task {
            for await newState in viewModel.uiState {
                state = newState
            }
        }
    }

    private var paywallContent: some View {
        VStack(spacing: 0) {
            closeRow
            ScrollView {
                VStack(spacing: 16) {
                    header
                    ComparisonTable(highlighted: state.feature)
                    planSection
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 8)
            }
            bottomActions
        }
    }

    private var closeRow: some View {
        HStack {
            Spacer()
            Button {
                viewModel.onClosed()
            } label: {
                Image(systemName: "xmark")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .padding(10)
            }
            .accessibilityLabel(String(localized: "Close"))
        }
        .padding(.horizontal, 8)
    }

    private var header: some View {
        VStack(spacing: 6) {
            ProCrown(size: 48)
            Text(PaywallCopy.headline(for: state.feature))
                .font(.title2.bold())
                .multilineTextAlignment(.center)
                .padding(.top, 4)
            Text(state.feature == .general
                ? String(localized: "Get more out of every recording.")
                : String(localized: "Part of Offhand Pro. Get more out of every recording."))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
    }

    @ViewBuilder
    private var planSection: some View {
        if state.isLoadingOffers {
            ProgressView().frame(maxWidth: .infinity).padding()
        } else if state.offers.isEmpty {
            VStack(spacing: 4) {
                Text(String(localized: "Prices could not be loaded. Check your connection and try again."))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                Button(String(localized: "Try again")) { viewModel.onRetryOffers() }
            }
        } else {
            HStack(alignment: .top, spacing: 12) {
                ForEach(state.offers, id: \.plan) { offer in
                    PlanCard(offer: offer, isSelected: offer.plan == state.selectedPlan) {
                        viewModel.onPlanSelected(plan: offer.plan)
                    }
                }
            }
        }
    }

    private var bottomActions: some View {
        VStack(spacing: 4) {
            Button {
                Haptics.confirm()
                viewModel.onPurchaseClicked()
            } label: {
                Text(PaywallCopy.cta(for: state.selectedOffer)).frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(state.selectedOffer == nil || state.isPurchasing)
            if let disclosure = PaywallCopy.disclosure(for: state.selectedOffer) {
                Text(disclosure).font(.caption).foregroundStyle(.secondary).multilineTextAlignment(.center)
            }
            Button(String(localized: "Continue with the free version")) { viewModel.onClosed() }
                .font(.footnote)
                .padding(.top, 2)
            Text(String(localized: "Payments go through the App Store. Offhand never sees your card, and your notes never leave your phone."))
                .font(.caption2)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            HStack(spacing: 16) {
                Button(String(localized: "Restore purchases")) { viewModel.onRestoreClicked() }
                    .disabled(state.isPurchasing)
                Link(String(localized: "Terms"), destination: URL(string: LegalLinks.shared.TERMS)!)
                Link(String(localized: "Privacy"), destination: URL(string: LegalLinks.shared.PRIVACY_POLICY)!)
            }
            .font(.footnote)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 10)
        .background(.bar)
    }

    private var successContent: some View {
        VStack(spacing: 12) {
            ProCrown(size: 72)
            Text(String(localized: "You're on Offhand Pro")).font(.title2.bold()).multilineTextAlignment(.center)
            Text(state.feature == .general
                ? String(localized: "Thank you for supporting an independent, private app.")
                : String(localized: "What you started is on its way."))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var messageBinding: Binding<Bool> {
        Binding(
            get: { state.message != nil },
            set: { isShown in if !isShown { viewModel.onMessageDismissed() } }
        )
    }

    private func messageText(_ message: PaywallMessageUi) -> String {
        switch onEnum(of: message) {
        case .failed: return String(localized: "The purchase did not go through and nothing was charged.")
        case .pending: return String(localized: "Your payment is still being processed. Pro unlocks as soon as it completes.")
        case .nothingToRestore: return String(localized: "No earlier purchase was found for this store account.")
        }
    }
}

private enum PaywallCopy {
    static func headline(for feature: ProFeature) -> String {
        switch feature {
        case .general: return String(localized: "Offhand Pro")
        case .customStyles: return String(localized: "Save your own note style")
        case .documentExport: return String(localized: "Share this note as PDF or Word")
        case .smartSuggestions: return String(localized: "Find dates and to-dos in your notes")
        case .audioImport: return String(localized: "Turn your recordings into notes")
        }
    }

    static func cta(for offer: ProOfferUi?) -> String {
        guard let offer else { return String(localized: "Start free trial") }
        switch (offer.plan, offer.trialDays > 0) {
        case (.lifetime, _): return String(localized: "Buy lifetime")
        case (_, true): return String.localizedStringWithFormat(String(localized: "Start %d-day free trial"), offer.trialDays)
        default: return String(localized: "Subscribe")
        }
    }

    static func disclosure(for offer: ProOfferUi?) -> String? {
        guard let offer, offer.plan == .yearly else { return nil }
        if offer.trialDays > 0 {
            let terms = String.localizedStringWithFormat(
                String(localized: "Free for %1$d days, then %2$@ per year. Cancel anytime in your store account."),
                offer.trialDays,
                offer.price
            )
            return String(localized: "No charge today") + "\n" + terms
        }
        return String(format: String(localized: "%@ per year, renews automatically. Cancel anytime in your store account."), offer.price)
    }
}

private struct ComparisonTable: View {
    let highlighted: ProFeature

    private struct Row: Identifiable {
        let id: Int
        let title: String
        let hint: String?
        let feature: ProFeature?
    }

    private static let columnWidth: CGFloat = 52

    private var rows: [Row] {
        [
            Row(id: 0, title: String(localized: "Private by design"), hint: nil, feature: nil),
            Row(id: 1, title: String(localized: "Fully offline"), hint: nil, feature: nil),
            Row(id: 2, title: String(localized: "No ads, no account"), hint: nil, feature: nil),
            Row(id: 3, title: String(localized: "Unlimited notes and recordings"), hint: nil, feature: nil),
            Row(id: 4, title: String(localized: "Encrypted backup, search and folders"), hint: nil, feature: nil),
            Row(id: 5, title: String(localized: "Notes in your format"), hint: String(localized: "Your headings, your sections, every time"), feature: .customStyles),
            Row(id: 6, title: String(localized: "Share polished documents"), hint: String(localized: "PDF and Word, ready for clients, email and print"), feature: .documentExport),
            Row(id: 7, title: String(localized: "Never miss a follow-up"), hint: String(localized: "Dates and to-dos straight into your calendar"), feature: .smartSuggestions),
            Row(id: 8, title: String(localized: "Turn any recording into a note"), hint: String(localized: "Voice memos, calls and files from other apps"), feature: .audioImport),
        ]
    }

    var body: some View {
        VStack(spacing: 0) {
            tableHeader
            ForEach(rows) { row in
                if row.id == 5 { Divider().padding(.horizontal, 12).padding(.vertical, 4) }
                tableRow(row)
            }
            Text(String(localized: "New Pro features are included as they ship"))
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
        }
        .padding(.vertical, 6)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private var tableHeader: some View {
        HStack(spacing: 0) {
            Spacer()
            Text(String(localized: "Offhand")).font(.caption.weight(.medium)).foregroundStyle(.secondary).frame(width: Self.columnWidth)
            HStack(spacing: 3) {
                ProCrown(size: 13)
                Text("Pro").font(.caption.weight(.semibold))
            }
            .frame(width: Self.columnWidth)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 4)
    }

    private func tableRow(_ row: Row) -> some View {
        let isHighlighted = row.feature != nil && row.feature == highlighted
        return HStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 2) {
                Text(row.title).font(isHighlighted ? .subheadline.weight(.semibold) : .subheadline)
                if let hint = row.hint {
                    Text(hint).font(.caption).foregroundStyle(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            checkCell(included: row.feature == nil, color: Brand.primary)
            checkCell(included: true, color: Brand.proGold)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 7)
        .background(isHighlighted ? Brand.primaryContainer : Color.clear, in: RoundedRectangle(cornerRadius: 10))
        .padding(.horizontal, 6)
    }

    private func checkCell(included: Bool, color: Color) -> some View {
        Group {
            if included {
                Image(systemName: "checkmark").font(.footnote.weight(.bold)).foregroundStyle(color)
            } else {
                Text("–").foregroundStyle(.tertiary)
            }
        }
        .frame(width: Self.columnWidth)
    }
}

private struct PlanCard: View {
    let offer: ProOfferUi
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text(title).font(.subheadline.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.8)
                    if offer.plan == .lifetime { bestValueBadge }
                    Spacer(minLength: 0)
                    if isSelected {
                        Image(systemName: "checkmark").font(.footnote.weight(.bold)).foregroundStyle(Brand.primary)
                    }
                }
                Text(price).font(.headline)
                Text(hint).font(.caption).foregroundStyle(.secondary)
                if let second = secondHint {
                    Text(second).font(.caption).foregroundStyle(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(isSelected ? Brand.primary : Color(.systemGray4), lineWidth: isSelected ? 2 : 1)
            )
        }
        .buttonStyle(.plain)
        .foregroundStyle(.primary)
    }

    private var bestValueBadge: some View {
        Text(String(localized: "Best value"))
            .font(.caption2.weight(.semibold))
            .lineLimit(1)
            .fixedSize()
            .foregroundStyle(Brand.proGold)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(Brand.proGold.opacity(0.18), in: RoundedRectangle(cornerRadius: 6))
    }

    private var title: String {
        offer.plan == .lifetime ? String(localized: "Lifetime") : String(localized: "Yearly")
    }

    private var price: String {
        offer.plan == .lifetime ? offer.price : String(format: String(localized: "%@ / year"), offer.price)
    }

    private var hint: String {
        if offer.plan == .lifetime { return String(localized: "Pay once, keep forever") }
        if offer.trialDays > 0 {
            return String.localizedStringWithFormat(String(localized: "%d-day free trial"), offer.trialDays)
        }
        return String(localized: "Renews every year")
    }

    private var secondHint: String? {
        if offer.plan == .yearly, let monthly = offer.monthlyPrice {
            return String(format: String(localized: "About %@ a month"), monthly)
        }
        if offer.plan == .lifetime, let years = offer.yearsOfYearly {
            return String.localizedStringWithFormat(String(localized: "Less than %d years of yearly"), years.int32Value)
        }
        return nil
    }
}
