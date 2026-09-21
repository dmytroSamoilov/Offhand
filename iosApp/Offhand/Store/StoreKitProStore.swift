import OffhandShared
import StoreKit
import UIKit

// StoreKit 2 behind the shared ProStore seam: the verified current
// entitlements decide the plan, Transaction.updates keeps it fresh, and
// nothing about the purchase ever leaves the App Store's own channel.
final class StoreKitProStore: IosProStoreBridge {
    private enum ProductId {
        static let yearly = "offhand_pro_yearly"
        static let lifetime = "offhand_pro_lifetime"
    }

    private var products: [String: Product] = [:]
    private var onStatus: ((ProStatus) -> Void)?
    private var updates: Task<Void, Never>?

    func start(onStatus: @escaping (ProStatus) -> Void) {
        self.onStatus = onStatus
        updates = Task {
            for await result in Transaction.updates {
                if case .verified(let transaction) = result { await transaction.finish() }
                await publishStatus()
            }
        }
        Task { await publishStatus() }
    }

    func loadOffers(onLoaded: @escaping ([ProOffer]) -> Void) {
        Task { onLoaded(await fetchOffers()) }
    }

    func purchase(plan: ProPlan, onResult: @escaping (PurchaseOutcome) -> Void) {
        Task { onResult(await buy(plan)) }
    }

    func restore(onDone: @escaping (ProStatus) -> Void) {
        Task {
            try? await AppStore.sync()
            let status = await currentStatus()
            onStatus?(status)
            onDone(status)
        }
    }

    func refresh() {
        Task { await publishStatus() }
    }

    private func publishStatus() async {
        onStatus?(await currentStatus())
    }

    private func fetchOffers() async -> [ProOffer] {
        let loaded = (try? await Product.products(for: [ProductId.yearly, ProductId.lifetime])) ?? []
        for product in loaded { products[product.id] = product }
        var offers: [ProOffer] = []
        if let yearly = products[ProductId.yearly] {
            offers.append(ProOffer(
                plan: .yearly,
                price: yearly.displayPrice,
                trialDays: await trialDays(of: yearly),
                priceMicros: micros(of: yearly),
                monthlyPrice: (yearly.price / 12).formatted(yearly.priceFormatStyle)
            ))
        }
        if let lifetime = products[ProductId.lifetime] {
            offers.append(ProOffer(plan: .lifetime, price: lifetime.displayPrice, trialDays: 0, priceMicros: micros(of: lifetime), monthlyPrice: nil))
        }
        return offers
    }

    private func micros(of product: Product) -> Int64 {
        Int64((product.price * 1_000_000 as NSDecimalNumber).doubleValue)
    }

    // A returning subscriber is no longer eligible for the introductory offer,
    // so the trial is only advertised when the store will actually grant it.
    private func trialDays(of product: Product) async -> Int32 {
        guard let subscription = product.subscription,
              let intro = subscription.introductoryOffer,
              intro.paymentMode == .freeTrial,
              await subscription.isEligibleForIntroOffer else { return 0 }
        return Int32(days(in: intro.period))
    }

    private func days(in period: Product.SubscriptionPeriod) -> Int {
        switch period.unit {
        case .day: return period.value
        case .week: return period.value * 7
        case .month: return period.value * 30
        case .year: return period.value * 365
        @unknown default: return 0
        }
    }

    private func buy(_ plan: ProPlan) async -> PurchaseOutcome {
        let id = plan == .lifetime ? ProductId.lifetime : ProductId.yearly
        guard let product = products[id] else { return .failed }
        do {
            switch try await product.purchase() {
            case .success(let verification):
                guard case .verified(let transaction) = verification else { return .failed }
                await transaction.finish()
                await publishStatus()
                return .purchased
            case .pending: return .pending
            case .userCancelled: return .cancelled
            @unknown default: return .failed
            }
        } catch {
            return .failed
        }
    }

    private func currentStatus() async -> ProStatus {
        var yearly: ProStatus?
        for await result in Transaction.currentEntitlements {
            guard case .verified(let transaction) = result, transaction.revocationDate == nil else { continue }
            if transaction.productID == ProductId.lifetime { return ProStatus.companion.LIFETIME }
            if transaction.productID == ProductId.yearly { yearly = yearlyStatus(of: transaction) }
        }
        return yearly ?? ProStatus.companion.FREE
    }

    private func yearlyStatus(of transaction: Transaction) -> ProStatus {
        let renewsAt = transaction.expirationDate.map { KotlinLong(value: Int64($0.timeIntervalSince1970 * 1000)) }
        return ProStatus(plan: .yearly, isTrial: transaction.offer?.type == .introductory, renewsAtMs: renewsAt)
    }
}

// Apple's in-app redemption sheet; the resulting transaction arrives through
// Transaction.updates like any purchase.
enum OfferCodeRedemption {
    static func present() {
        guard let scene = UIWindowScene.foregroundActive else { return }
        Task { try? await AppStore.presentOfferCodeRedeemSheet(in: scene) }
    }
}

// Apple's in-app subscription management sheet, where the user can cancel
// without leaving the app, which App Review expects to be reachable.
enum SubscriptionManagement {
    static func present() {
        guard let scene = UIWindowScene.foregroundActive else { return }
        Task { try? await AppStore.showManageSubscriptions(in: scene) }
    }
}

private extension UIWindowScene {
    static var foregroundActive: UIWindowScene? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
    }
}
