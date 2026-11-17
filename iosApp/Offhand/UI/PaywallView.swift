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
        mode: .opening,
        message: nil,
        trialRecap: []
    )

    private static let successDismissDelay: Duration = .milliseconds(1600)

    var body: some View {
        Group {
            switch state.mode {
            case .opening:
                Color.clear
            case .benefits:
                benefitsContent
            case .offer:
                if state.isPro { successContent } else { paywallContent }
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
            guard state.isPro, state.mode == .offer else { return }
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
                    if !state.trialRecap.isEmpty { trialRecap }
                    ComparisonTable(highlighted: state.feature, isCollapsible: true)
                    planSection
                    legalRow
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
            }
            if state.feature == .freeTrial {
                trialIntroActions
            } else {
                bottomActions
            }
        }
    }

    private var benefitsContent: some View {
        VStack(spacing: 0) {
            closeRow
            ScrollView {
                VStack(spacing: 16) {
                    VStack(spacing: 6) {
                        ProCrown(size: 48)
                        Text(String(localized: "You're on Offhand Pro"))
                            .font(.title2.bold())
                            .multilineTextAlignment(.center)
                            .padding(.top, 4)
                        Text(String(localized: "Everything in the Pro column is yours."))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    ComparisonTable(highlighted: .general, isCollapsible: false)
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 20)
            }
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
                    .foregroundStyle(.primary)
                    .padding(6)
            }
            .modifier(CircleButton())
            .accessibilityLabel(String(localized: "Close"))
        }
        .padding(.horizontal, 16)
    }

    private var header: some View {
        VStack(spacing: 6) {
            ProCrown(size: 48)
            Text(PaywallCopy.headline(for: state))
                .font(.title2.bold())
                .multilineTextAlignment(.center)
                .padding(.top, 4)
            Text(PaywallCopy.tagline(for: state))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
    }

    // What the free trial was used for, shown back on the paywall that
    // follows it.
    private var trialRecap: some View {
        VStack(spacing: 8) {
            ForEach(state.trialRecap, id: \.usage) { line in
                HStack {
                    Text(PaywallCopy.label(for: line.usage))
                    Spacer(minLength: 12)
                    Text("\(line.count)").font(.title2.bold())
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Brand.proGold.opacity(0.12), in: RoundedRectangle(cornerRadius: Brand.cardRadius))
    }

    // The welcome to the fourteen free days: starting them is the main
    // action and costs nothing, buying a plan right away sits under it.
    private var trialIntroActions: some View {
        VStack(spacing: 8) {
            Button {
                Haptics.confirm()
                viewModel.onClosed()
            } label: {
                Text(PaywallCopy.startTrial).frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .disabled(state.isPurchasing)
            Button {
                viewModel.onPurchaseClicked()
            } label: {
                Text(PaywallCopy.buyNow(for: state.selectedOffer)).frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .controlSize(.large)
            .disabled(state.selectedOffer == nil || state.isPurchasing)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(.bar)
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
            VStack(spacing: 8) {
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
            .controlSize(.large)
            .disabled(state.selectedOffer == nil || state.isPurchasing)
            if let disclosure = PaywallCopy.disclosure(for: state.selectedOffer) {
                Text(disclosure).font(.caption).foregroundStyle(.secondary).multilineTextAlignment(.center)
            }
            Button(String(localized: "Continue with the free version")) { viewModel.onClosed() }
                .font(.footnote)
                .padding(.top, 2)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(.bar)
    }

    private var legalRow: some View {
        HStack(spacing: 16) {
            Button(String(localized: "Restore purchases")) { viewModel.onRestoreClicked() }
                .disabled(state.isPurchasing)
            Link(String(localized: "Terms"), destination: URL(string: LegalLinks.shared.TERMS)!)
            Link(String(localized: "Privacy"), destination: URL(string: LegalLinks.shared.PRIVACY_POLICY)!)
        }
        .font(.footnote)
        .frame(maxWidth: .infinity)
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
    static func headline(for state: PaywallUiState) -> String {
        guard state.feature == .trialEnded, !state.trialRecap.isEmpty else { return headline(for: state.feature) }
        return String(localized: "Your 14 days with Offhand Pro")
    }

    static func tagline(for state: PaywallUiState) -> String {
        guard state.feature == .trialEnded else { return tagline(for: state.feature) }
        return state.trialRecap.isEmpty
            ? String(localized: "Your 14 days of Offhand Pro are over. Your notes, folders and styles are all still here. You can keep using the free version, or upgrade to keep everything in Pro.")
            : String(localized: "Your trial has ended. Keep all of this with Pro.")
    }

    static func label(for usage: ProTrialUsage) -> String {
        switch usage {
        case .meetingNotes: return String(localized: "Meeting notes")
        case .visitNotes: return String(localized: "Visit report")
        case .legalNotes: return String(localized: "Legal note")
        case .emailNotes: return String(localized: "Email")
        case .customStyleNotes: return String(localized: "Notes in your own styles")
        case .pdfExports: return String(localized: "PDFs shared")
        case .wordExports: return String(localized: "Word documents shared")
        case .imports: return String(localized: "Recordings and videos imported")
        case .calendarEvents: return String(localized: "Events added to your calendar")
        case .folderStyles: return String(localized: "Folders with their own style")
        case .audioBackups: return String(localized: "Backups with your recordings")
        }
    }

    static func headline(for feature: ProFeature) -> String {
        switch feature {
        case .general: return String(localized: "Offhand Pro")
        case .customStyles: return String(localized: "Save your own writing style")
        case .documentExport: return String(localized: "Share this note as PDF or Word")
        case .smartSuggestions: return String(localized: "Find dates and to-dos in your notes")
        case .audioImport: return String(localized: "Turn recordings and videos into notes")
        case .folderStyles: return String(localized: "Give this folder its own writing style")
        case .noteStyles: return String(localized: "Choose a different writing style")
        case .folders: return String(localized: "Keep more than two folders")
        case .backupAudio: return String(localized: "Back up your recordings too")
        case .widget: return String(localized: "Record from your Home Screen")
        case .freeTrial: return String(localized: "Try Offhand Pro free for 14 days")
        case .trialEnded: return String(localized: "Your Pro trial has ended")
        }
    }

    static func tagline(for feature: ProFeature) -> String {
        switch feature {
        case .general: return String(localized: "Get more out of every recording.")
        case .freeTrial: return String(localized: "Everything in Pro is unlocked from today. No payment and no account. After 14 days you keep the free version unless you upgrade.")
        default: return String(localized: "Part of Offhand Pro. Get more out of every recording.")
        }
    }

    static var startTrial: String {
        String.localizedStringWithFormat(String(localized: "Start %d-day free trial"), Int(ProTrialPolicy.shared.DURATION_DAYS))
    }

    static func buyNow(for offer: ProOfferUi?) -> String {
        offer?.plan == .lifetime ? String(localized: "Buy lifetime") : String(localized: "Subscribe")
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
        guard let offer, offer.plan == .yearly || offer.plan == .monthly else { return nil }
        if offer.plan == .monthly {
            return String(format: String(localized: "%@ per month, renews automatically. Cancel anytime in your store account."), offer.price)
        }
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
    let isCollapsible: Bool

    @State private var isExpanded = false

    private static let collapsedHeight: CGFloat = 200

    private struct Row: Identifiable {
        let id: Int
        let title: String
        let hint: String?
        let features: Set<ProFeature>

        var isPro: Bool { !features.isEmpty }
    }

    private static let columnWidth: CGFloat = 52

    // The row the paywall was opened for and the Pro rows come first, so the
    // part that shows while collapsed is worth reading.
    private var rows: [Row] {
        allRows.enumerated().sorted { first, second in
            let firstRank = rank(of: first.element)
            let secondRank = rank(of: second.element)
            return firstRank == secondRank ? first.offset < second.offset : firstRank < secondRank
        }.map(\.element)
    }

    private func rank(of row: Row) -> Int {
        if row.features.contains(highlighted) { return 0 }
        return row.isPro ? 1 : 2
    }

    private var allRows: [Row] {
        [
            Row(id: 11, title: String(localized: "Transcript and Summary on every note"), hint: nil, features: []),
            Row(id: 0, title: String(localized: "Private by design"), hint: nil, features: []),
            Row(id: 1, title: String(localized: "Fully offline"), hint: nil, features: []),
            Row(id: 2, title: String(localized: "No ads, no account"), hint: nil, features: []),
            Row(id: 3, title: String(localized: "Unlimited notes and recordings"), hint: nil, features: []),
            Row(id: 4, title: String(localized: "Encrypted backup, search and two folders"), hint: nil, features: []),
            Row(id: 5, title: String(localized: "Notes in your format"), hint: String(localized: "Meeting notes, Visit report, Legal note, your own styles, and a new style for any note"), features: [.customStyles, .noteStyles]),
            Row(id: 6, title: String(localized: "Share polished documents"), hint: String(localized: "PDF and Word, ready for clients, email and print"), features: [.documentExport]),
            Row(id: 7, title: String(localized: "Never miss a follow-up"), hint: String(localized: "Dates and to-dos straight into your calendar"), features: [.smartSuggestions]),
            Row(id: 8, title: String(localized: "Turn any recording or video into a note"), hint: String(localized: "Voice memos, calls, screen recordings and video files without limit (three are free to try)"), features: [.audioImport]),
            Row(id: 9, title: String(localized: "As many folders as you need"), hint: String(localized: "Each with its own writing style"), features: [.folderStyles, .folders]),
            Row(id: 10, title: String(localized: "Backups with your recordings"), hint: String(localized: "Audio included, still encrypted"), features: [.backupAudio]),
        ]
    }

    private var isCollapsed: Bool { isCollapsible && !isExpanded }

    // Collapsed, the table shows its first rows fading out under a "see
    // everything" control, so the plans sit higher on the screen.
    var body: some View {
        VStack(spacing: 0) {
            tableContent
                .frame(height: isCollapsed ? Self.collapsedHeight : nil, alignment: .top)
                .clipped()
                .overlay(alignment: .bottom) {
                    if isCollapsed { expandControl }
                }
            if isCollapsible && isExpanded {
                Button {
                    withAnimation { isExpanded = false }
                } label: {
                    Label(String(localized: "Show less"), systemImage: "chevron.up")
                        .labelStyle(.titleAndIcon)
                        .font(.footnote.weight(.semibold))
                }
                .padding(.bottom, 10)
            }
        }
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: Brand.cardRadius))
        .clipShape(RoundedRectangle(cornerRadius: Brand.cardRadius))
    }

    private var expandControl: some View {
        Button {
            withAnimation { isExpanded = true }
        } label: {
            HStack(spacing: 4) {
                Text(String(localized: "See everything in Pro"))
                Image(systemName: "chevron.down")
            }
            .font(.footnote.weight(.semibold))
            .foregroundStyle(Brand.primary)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
            .padding(.bottom, 10)
            .background(
                LinearGradient(
                    stops: [
                        .init(color: Color(.secondarySystemGroupedBackground).opacity(0), location: 0.3),
                        .init(color: Color(.secondarySystemGroupedBackground), location: 0.8),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private var tableContent: some View {
        VStack(spacing: 0) {
            tableHeader
            ForEach(Array(rows.enumerated()), id: \.element.id) { index, row in
                if index > 0 && row.isPro != rows[index - 1].isPro {
                    Divider().padding(.horizontal, 12).padding(.vertical, 4)
                }
                tableRow(row)
            }
            Text(String(localized: "New Pro features are included as they ship"))
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
            Text(String(localized: "Payments go through the App Store. Offhand never sees your card, and your notes never leave your iPhone."))
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 12)
                .padding(.bottom, 8)
        }
        .padding(.vertical, 6)
        .fixedSize(horizontal: false, vertical: true)
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
        let isHighlighted = row.features.contains(highlighted)
        return HStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 2) {
                Text(row.title).font(isHighlighted ? .subheadline.weight(.semibold) : .subheadline)
                if let hint = row.hint {
                    Text(hint).font(.caption).foregroundStyle(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            checkCell(included: !row.isPro, color: Brand.primary)
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

    // One row per plan, stacked, so a third plan and long prices in any
    // language still fit: the selection mark, the plan with what it means,
    // and the price at the end.
    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundStyle(isSelected ? Brand.primary : Color(.systemGray3))
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 8) {
                        Text(title).font(.subheadline.weight(.semibold)).lineLimit(1)
                        if offer.plan == .lifetime { bestValueBadge }
                    }
                    Text(hint).font(.caption).foregroundStyle(.secondary)
                    if let second = secondHint {
                        Text(second).font(.caption).foregroundStyle(.secondary)
                    }
                }
                Spacer(minLength: 8)
                Text(price).font(.headline).multilineTextAlignment(.trailing)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: Brand.cardRadius))
            .overlay(
                RoundedRectangle(cornerRadius: Brand.cardRadius)
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
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(Brand.proGold.opacity(0.18), in: Capsule())
    }

    private var title: String {
        switch offer.plan {
        case .lifetime: return String(localized: "Lifetime")
        case .monthly: return String(localized: "Monthly")
        default: return String(localized: "Yearly")
        }
    }

    private var price: String {
        switch offer.plan {
        case .lifetime: return offer.price
        case .monthly: return String(format: String(localized: "%@ / month"), offer.price)
        default: return String(format: String(localized: "%@ / year"), offer.price)
        }
    }

    private var hint: String {
        if offer.plan == .lifetime { return String(localized: "Pay once, keep forever") }
        if offer.trialDays > 0 {
            return String.localizedStringWithFormat(String(localized: "%d-day free trial"), offer.trialDays)
        }
        return offer.plan == .monthly ? String(localized: "Renews every month") : String(localized: "Renews every year")
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

// The system toolbar draws its buttons as circles on glass; the paywall has
// no toolbar, so its close button borrows the same shape.
private struct CircleButton: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.buttonStyle(.glass).buttonBorderShape(.circle)
        } else {
            content.buttonStyle(.bordered).buttonBorderShape(.circle)
        }
    }
}
