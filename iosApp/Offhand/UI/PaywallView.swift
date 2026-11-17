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
        trialRecap: [],
        trialDaysLeft: nil,
        isSummaryAvailable: true
    )
    @State private var isComparisonExpanded = false

    private static let successDismissDelay: Duration = .milliseconds(1600)

    var body: some View {
        Group {
            switch state.mode {
            case .opening:
                Color.clear
            case .benefits:
                benefitsContent
            case .offer:
                if state.isPro { successContent } else { offerContent }
            }
        }
        .foregroundStyle(OffhandColor.ink)
        .background(OffhandColor.ground.ignoresSafeArea())
        .offhandDialog(isPresented: messageBinding) { messageDialog }
        .environment(\.colorScheme, .dark)
        .preferredColorScheme(.dark)
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

    private var offerContent: some View {
        VStack(spacing: 0) {
            topBar(showsRestore: true)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    PaywallHeader(
                        title: PaywallCopy.headline(for: state, recap: trialRecap),
                        message: PaywallCopy.tagline(for: state.feature, recap: trialRecap)
                    )
                    if !trialRecap.isEmpty {
                        TrialRecap(lines: trialRecap).padding(.top, 22)
                    }
                    BenefitLines(titles: comparisonRows.filter(\.isPro).prefix(3).map(\.title))
                        .padding(.top, 22)
                    planSection.padding(.top, OffhandSpacing.screen)
                    comparison.padding(.top, 12)
                    legalLinks.padding(.top, 8)
                }
                .padding(.horizontal, OffhandSpacing.screen)
                .padding(.bottom, OffhandSpacing.card)
            }
            if state.feature == .freeTrial {
                trialIntroActions
            } else {
                offerActions
            }
        }
    }

    private var benefitsContent: some View {
        VStack(spacing: 0) {
            topBar(showsRestore: false)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    PaywallHeader(
                        title: String(localized: "You're on Offhand Pro"),
                        message: String(localized: "Everything in the Pro column is yours.")
                    )
                    ComparisonTable(
                        rows: ComparisonRows.sorted(for: .general, isSummaryAvailable: state.isSummaryAvailable),
                        highlighted: .general
                    )
                    .padding(.top, OffhandSpacing.screen)
                }
                .padding(.horizontal, OffhandSpacing.screen)
                .padding(.bottom, OffhandSpacing.screen)
            }
        }
    }

    private var comparisonRows: [ComparisonRows.Row] {
        ComparisonRows.sorted(for: state.feature, isSummaryAvailable: state.isSummaryAvailable)
    }

    private var trialRecap: [ProTrialUsageCount] {
        state.isSummaryAvailable ? state.trialRecap : state.trialRecap.filter { !PaywallCopy.needsSummaries($0.usage) }
    }

    private func topBar(showsRestore: Bool) -> some View {
        HStack {
            BarIconButton(systemImage: "xmark", label: String(localized: "Close")) { viewModel.onClosed() }
            Spacer()
            if showsRestore {
                Button(String(localized: "Restore purchases")) { viewModel.onRestoreClicked() }
                    .buttonStyle(.offhandPlainText(.muted))
                    .disabled(state.isPurchasing)
            }
        }
        .padding(.horizontal, 12)
        .padding(.top, 4)
    }

    @ViewBuilder
    private var planSection: some View {
        if state.isLoadingOffers {
            ProgressView()
                .tint(OffhandColor.ink2)
                .frame(maxWidth: .infinity, minHeight: 64)
        } else if state.offers.isEmpty {
            VStack(alignment: .leading, spacing: 4) {
                Text(String(localized: "Prices could not be loaded. Check your connection and try again."))
                    .offhandText(.bodySmall)
                    .foregroundStyle(OffhandColor.ink2)
                PlainTextButton(String(localized: "Try again")) { viewModel.onRetryOffers() }
                    .padding(.leading, -OffhandSpacing.controls)
            }
        } else {
            VStack(spacing: 8) {
                ForEach(state.offers, id: \.plan) { offer in
                    PlanRow(offer: offer, isSelected: offer.plan == state.selectedPlan) {
                        Haptics.selection()
                        viewModel.onPlanSelected(plan: offer.plan)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var comparison: some View {
        if isComparisonExpanded {
            VStack(spacing: 4) {
                ComparisonTable(rows: comparisonRows, highlighted: state.feature)
                PlainTextButton(String(localized: "Show less"), tone: .muted, systemImage: "chevron.up") {
                    withAnimation(OffhandMotion.state) { isComparisonExpanded = false }
                }
            }
        } else {
            PlainTextButton(String(localized: "See everything in Pro"), systemImage: "chevron.down") {
                withAnimation(OffhandMotion.state) { isComparisonExpanded = true }
            }
            .padding(.leading, -OffhandSpacing.controls)
        }
    }

    private var legalLinks: some View {
        HStack(spacing: 20) {
            Link(String(localized: "Terms"), destination: URL(string: LegalLinks.shared.TERMS)!)
            Link(String(localized: "Privacy"), destination: URL(string: LegalLinks.shared.PRIVACY_POLICY)!)
        }
        .offhandText(.caption, face: .interMedium)
        .foregroundStyle(OffhandColor.ink3)
        .frame(minHeight: OffhandSpacing.touchTarget)
    }

    private var offerActions: some View {
        VStack(spacing: 4) {
            GoldButton(PaywallCopy.cta(for: state.selectedOffer), fillsWidth: true) {
                Haptics.confirm()
                viewModel.onPurchaseClicked()
            }
            .disabled(state.selectedOffer == nil || state.isPurchasing)
            PlainTextButton(String(localized: "Continue with the free version"), tone: .muted, fillsWidth: true) {
                viewModel.onClosed()
            }
            if let disclosure = PaywallCopy.disclosure(for: state.selectedOffer) {
                Text(disclosure)
                    .font(.offhand(.interRegular, size: 12, relativeTo: .caption))
                    .foregroundStyle(OffhandColor.ink3)
                    .multilineTextAlignment(.center)
            }
        }
        .padding(.horizontal, OffhandSpacing.screen)
        .padding(.top, 12)
        .padding(.bottom, 8)
    }

    private var trialIntroActions: some View {
        VStack(spacing: 8) {
            GoldButton(String(localized: "Keep using Pro for free"), fillsWidth: true) {
                Haptics.confirm()
                viewModel.onClosed()
            }
            .disabled(state.isPurchasing)
            SecondaryButton(PaywallCopy.buyNow(for: state.selectedOffer), fillsWidth: true) {
                viewModel.onPurchaseClicked()
            }
            .disabled(state.selectedOffer == nil || state.isPurchasing)
        }
        .padding(.horizontal, OffhandSpacing.screen)
        .padding(.top, 12)
        .padding(.bottom, 8)
    }

    private var successContent: some View {
        VStack(spacing: 12) {
            ProCrown(size: 56)
            Text(String(localized: "You're on Offhand Pro"))
                .offhandText(.title)
            Text(state.feature == .general
                ? String(localized: "Thank you for supporting an independent, private app.")
                : String(localized: "What you started is on its way."))
                .offhandText(.body)
                .foregroundStyle(OffhandColor.ink2)
        }
        .multilineTextAlignment(.center)
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var messageDialog: some View {
        OffhandDialog(
            title: String(localized: "Offhand Pro"),
            message: state.message.map(messageText),
            confirmLabel: String(localized: "OK"),
            onConfirm: { viewModel.onMessageDismissed() },
            onDismiss: { viewModel.onMessageDismissed() }
        )
        .environment(\.colorScheme, .dark)
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

private struct PaywallHeader: View {
    let title: String
    let message: String

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 10) {
                ProCrown(size: 22).accessibilityHidden(true)
                Text(String(localized: "Offhand Pro"))
                    .offhandText(.label)
                    .foregroundStyle(OffhandColor.gold)
            }
            .padding(.top, 20)
            Text(title)
                .offhandText(.title)
                .padding(.top, 14)
                .accessibilityAddTraits(.isHeader)
            Text(message)
                .offhandText(.body)
                .foregroundStyle(OffhandColor.ink2)
                .padding(.top, 10)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct BenefitLines: View {
    let titles: [String]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(titles, id: \.self) { title in
                HStack(alignment: .firstTextBaseline, spacing: 12) {
                    GoldCheck()
                    Text(title).offhandText(.dialogBody)
                }
            }
        }
    }
}

private struct GoldCheck: View {
    var body: some View {
        Image(systemName: "checkmark")
            .font(.system(size: 14, weight: .bold))
            .foregroundStyle(OffhandColor.gold)
            .frame(width: 18)
            .accessibilityHidden(true)
    }
}

private struct TrialRecap: View {
    let lines: [ProTrialUsageCount]

    var body: some View {
        SurfaceCard {
            ForEach(Array(lines.enumerated()), id: \.element.usage) { index, line in
                HStack(spacing: 12) {
                    Text(PaywallCopy.label(for: line.usage))
                        .offhandText(.settingTitle)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(verbatim: "\(line.count)")
                        .font(.offhand(.monoMedium, size: 20, relativeTo: .title3))
                        .foregroundStyle(OffhandColor.gold)
                }
                .padding(.horizontal, OffhandSpacing.card)
                .frame(minHeight: 52)
                if index < lines.count - 1 { Hairline() }
            }
        }
    }
}

private struct PlanRow: View {
    let offer: ProOfferUi
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 16, style: .continuous)
        Button(action: action) {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 8) {
                        Text(PaywallCopy.planTitle(offer.plan))
                            .offhandText(.body, face: .interSemibold)
                        if offer.plan == .lifetime { bestValueTag }
                    }
                    Text(PaywallCopy.planHint(offer))
                        .offhandText(.caption)
                        .foregroundStyle(OffhandColor.ink2)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Text(offer.price)
                    .font(.offhand(.monoRegular, size: 15, relativeTo: .subheadline))
                    .multilineTextAlignment(.trailing)
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 10)
            .frame(minHeight: 64)
            .background(OffhandColor.well, in: shape)
            .overlay {
                shape.strokeBorder(isSelected ? OffhandColor.gold : OffhandColor.border, lineWidth: isSelected ? 1.5 : 1)
            }
            .contentShape(shape)
        }
        .buttonStyle(.offhandPress)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .animation(OffhandMotion.state, value: isSelected)
    }

    private var bestValueTag: some View {
        Text(String(localized: "Best value"))
            .font(.offhand(.monoMedium, size: 11, relativeTo: .caption2))
            .textCase(.uppercase)
            .foregroundStyle(OffhandColor.ink2)
            .lineLimit(1)
            .fixedSize()
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .overlay { RoundedRectangle(cornerRadius: 6, style: .continuous).strokeBorder(OffhandColor.border, lineWidth: 1) }
    }
}

private struct ComparisonTable: View {
    let rows: [ComparisonRows.Row]
    let highlighted: ProFeature

    private static let columnWidth: CGFloat = 60

    var body: some View {
        SurfaceCard {
            header
            ForEach(Array(rows.enumerated()), id: \.element.id) { index, row in
                if index > 0 && row.isPro != rows[index - 1].isPro {
                    Hairline().padding(.vertical, 4)
                }
                tableRow(row)
            }
            VStack(alignment: .leading, spacing: 8) {
                Text(String(localized: "New Pro features are included as they ship"))
                Text(String(localized: "Payments go through the App Store. Offhand never sees your card, and your notes never leave your iPhone."))
            }
            .offhandText(.caption)
            .foregroundStyle(OffhandColor.ink2)
            .padding(.horizontal, OffhandSpacing.card)
            .padding(.vertical, 12)
        }
    }

    private var header: some View {
        HStack(spacing: 0) {
            Spacer()
            Text(String(localized: "Offhand"))
                .foregroundStyle(OffhandColor.ink2)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .frame(width: Self.columnWidth)
            Text(verbatim: "Pro")
                .foregroundStyle(OffhandColor.gold)
                .frame(width: Self.columnWidth)
        }
        .offhandText(.caption, face: .interSemibold)
        .padding(.horizontal, 8)
        .padding(.top, 12)
        .padding(.bottom, 4)
    }

    private func tableRow(_ row: ComparisonRows.Row) -> some View {
        let isHighlighted = row.features.contains(highlighted)
        return HStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 2) {
                Text(row.title)
                    .offhandText(.settingTitle, face: isHighlighted ? .interSemibold : .interMedium)
                if let hint = row.hint {
                    Text(hint)
                        .offhandText(.caption)
                        .foregroundStyle(OffhandColor.ink2)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            checkCell(isIncluded: !row.isPro, color: OffhandColor.ink2)
            checkCell(isIncluded: true, color: OffhandColor.gold)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(isHighlighted ? OffhandColor.well : .clear, in: RoundedRectangle(cornerRadius: OffhandRadius.control))
        .padding(.horizontal, 6)
    }

    private func checkCell(isIncluded: Bool, color: Color) -> some View {
        Group {
            if isIncluded {
                Image(systemName: "checkmark").font(.system(size: 13, weight: .bold)).foregroundStyle(color)
            } else {
                Text(verbatim: "–").foregroundStyle(OffhandColor.ink3)
            }
        }
        .frame(width: Self.columnWidth)
    }
}

private enum ComparisonRows {
    struct Row: Identifiable {
        let id: Int
        let title: String
        let hint: String?
        let features: Set<ProFeature>
        var needsSummaries = false

        var isPro: Bool { !features.isEmpty }
    }

    static func sorted(for highlighted: ProFeature, isSummaryAvailable: Bool) -> [Row] {
        let rows = isSummaryAvailable ? all : transcriptsOnly
        return rows.enumerated().sorted { first, second in
            let firstRank = rank(of: first.element, highlighted: highlighted)
            let secondRank = rank(of: second.element, highlighted: highlighted)
            return firstRank == secondRank ? first.offset < second.offset : firstRank < secondRank
        }.map(\.element)
    }

    private static func rank(of row: Row, highlighted: ProFeature) -> Int {
        if row.features.contains(highlighted) { return 0 }
        return row.isPro ? 1 : 2
    }

    private static var transcriptsOnly: [Row] {
        all.filter { !$0.needsSummaries }.map { row in
            row.features.contains(.folderStyles)
                ? Row(id: row.id, title: row.title, hint: nil, features: row.features)
                : row
        }
    }

    private static var all: [Row] {
        [
            Row(id: 11, title: String(localized: "Transcript and Summary on every note"), hint: nil, features: [], needsSummaries: true),
            Row(id: 0, title: String(localized: "Private by design"), hint: nil, features: []),
            Row(id: 1, title: String(localized: "Fully offline"), hint: nil, features: []),
            Row(id: 2, title: String(localized: "No ads, no account"), hint: nil, features: []),
            Row(id: 3, title: String(localized: "Unlimited notes and recordings"), hint: nil, features: []),
            Row(id: 4, title: String(localized: "Encrypted backup, search and two folders"), hint: nil, features: []),
            Row(id: 5, title: String(localized: "Notes in your format"), hint: String(localized: "Meeting notes, Visit report, Legal note, your own styles, and a new style for any note"), features: [.customStyles, .noteStyles], needsSummaries: true),
            Row(id: 6, title: String(localized: "Share polished documents"), hint: String(localized: "PDF and Word, ready for clients, email and print"), features: [.documentExport]),
            Row(id: 7, title: String(localized: "Never miss a follow-up"), hint: String(localized: "Dates and to-dos straight into your calendar"), features: [.smartSuggestions], needsSummaries: true),
            Row(id: 8, title: String(localized: "Turn any recording or video into a note"), hint: String(localized: "Voice memos, calls, screen recordings and video files without limit (three are free to try)"), features: [.audioImport]),
            Row(id: 9, title: String(localized: "As many folders as you need"), hint: String(localized: "Each with its own writing style"), features: [.folderStyles, .folders]),
            Row(id: 10, title: String(localized: "Backups with your recordings"), hint: String(localized: "Audio included, still encrypted"), features: [.backupAudio]),
        ]
    }
}

private enum PaywallCopy {
    static func headline(for state: PaywallUiState, recap: [ProTrialUsageCount]) -> String {
        if state.feature == .freeTrial, let daysLeft = state.trialDaysLeft {
            return String.localizedStringWithFormat(String(localized: "Offhand Pro is yours free for %lld more days"), daysLeft.int64Value)
        }
        guard state.feature == .trialEnded, !recap.isEmpty else { return headline(for: state.feature) }
        return String(localized: "Your 14 days with Offhand Pro")
    }

    static func tagline(for feature: ProFeature, recap: [ProTrialUsageCount]) -> String {
        guard feature == .trialEnded else { return tagline(for: feature) }
        return recap.isEmpty
            ? String(localized: "Your 14 days of Offhand Pro are over. Your notes, folders and styles are all still here. You can keep using the free version, or upgrade to keep everything in Pro.")
            : String(localized: "Your trial has ended. Keep all of this with Pro.")
    }

    static func needsSummaries(_ usage: ProTrialUsage) -> Bool {
        switch usage {
        case .meetingNotes, .visitNotes, .legalNotes, .emailNotes, .customStyleNotes, .calendarEvents, .folderStyles:
            return true
        case .pdfExports, .wordExports, .imports, .audioBackups:
            return false
        }
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
        case .freeTrial: return String(localized: "Offhand Pro")
        case .trialEnded: return String(localized: "Your Pro trial has ended")
        }
    }

    static func tagline(for feature: ProFeature) -> String {
        switch feature {
        case .general: return String(localized: "Get more out of every recording.")
        case .freeTrial: return String(localized: "Everything in Pro is already unlocked. No payment and no account. When the trial ends you keep the free version unless you upgrade.")
        default: return String(localized: "Part of Offhand Pro. Get more out of every recording.")
        }
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

    static func planTitle(_ plan: ProPlan) -> String {
        switch plan {
        case .lifetime: return String(localized: "Lifetime")
        case .monthly: return String(localized: "Monthly")
        default: return String(localized: "Yearly")
        }
    }

    static func planHint(_ offer: ProOfferUi) -> String {
        if offer.plan == .lifetime { return String(localized: "Pay once, keep forever") }
        if offer.plan == .yearly, let monthly = offer.monthlyPrice {
            return String(format: String(localized: "About %@ a month"), monthly)
        }
        if offer.trialDays > 0 {
            return String.localizedStringWithFormat(String(localized: "%d-day free trial"), offer.trialDays)
        }
        return offer.plan == .monthly ? String(localized: "Renews every month") : String(localized: "Renews every year")
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
