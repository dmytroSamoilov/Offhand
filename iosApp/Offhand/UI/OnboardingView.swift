import OffhandShared
import SwiftUI

struct OnboardingView: View {
    let onFinished: () -> Void
    @Environment(\.scenePhase) private var scenePhase
    private let viewModel = AppViewModels.onboarding
    @State private var visiblePage = 0
    @State private var state = OnboardingUiState(
        step: .deviceCheck,
        deviceSpecs: nil,
        downloadSizeGb: "",
        notePreset: .summary,
        isDeviceSecure: false,
        isAppLockEnabled: true,
        isTelemetryEnabled: true,
        currentPage: 0,
        pages: [],
        furthestPage: 0
    )

    var body: some View {
        content
            .background(Brand.surface.ignoresSafeArea())
            .onChange(of: scenePhase) {
                if scenePhase == .active { viewModel.onDeviceLockRecheck() }
            }
            .task {
                for await newState in viewModel.uiState {
                    state = newState
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        switch state.step {
        case .deviceCheck:
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .deviceIncompatible:
            incompatibleScreen
        default:
            wizard
        }
    }

    // All pages sit in the pager up front — growing it while a fling settles
    // makes the leftover momentum cascade across several pages at once. Paged
    // scrolling moves one card per gesture, so settling past the furthest
    // unlocked page acts as Continue for the card just crossed; only the final
    // download card is button-only, because its Continue starts the download
    // and ends onboarding. The dots and the continue button stay put.
    private var wizard: some View {
        VStack(spacing: 0) {
            TabView(selection: $visiblePage) {
                ForEach(Array(state.pages.enumerated()), id: \.offset) { index, step in
                    stepCard(for: step)
                        .padding(.horizontal, 24)
                        .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .onChange(of: visiblePage) {
                if visiblePage > Int(state.furthestPage) {
                    continueAction(for: state.pages[visiblePage - 1])?()
                } else {
                    viewModel.onPageSelected(page: Int32(visiblePage))
                }
            }
            .onChange(of: state.currentPage) {
                guard visiblePage != Int(state.currentPage) else { return }
                withAnimation(.easeInOut(duration: 0.28)) {
                    visiblePage = Int(state.currentPage)
                }
            }
            if state.pageCount > 1 {
                PageDots(currentPage: Int(state.currentPage), pageCount: Int(state.pageCount))
                    .padding(.top, 24)
            }
            if let continueAction = continueAction(for: state.step) {
                Button(action: continueAction) {
                    Text(continueTitle)
                        .font(.headline)
                        .foregroundStyle(Brand.onPrimary)
                        .frame(maxWidth: .infinity, minHeight: 56)
                        .background(Brand.primary, in: Capsule())
                        .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .padding(.top, 20)
                .padding(.horizontal, 24)
            }
        }
        .padding(.vertical, 24)
    }

    @ViewBuilder
    private func stepCard(for step: OnboardingStep) -> some View {
        switch step {
        case .privacy:
            StepCard(
                icon: "lock.shield.fill",
                title: String(localized: "Private by design"),
                message: String(localized: "Recordings are transcribed and organized entirely on this iPhone. Nothing ever leaves your device.")
            )
        case .noteStyle:
            StepCard(
                icon: "text.badge.checkmark",
                title: String(localized: "How should notes be organized?"),
                message: String(localized: "Pick a default style. You can change it for any note later."),
                content: {
                    NotePresetPicker(selected: state.notePreset) { preset in
                        viewModel.onNoteStyleSelected(preset: preset)
                    }
                }
            )
        case .deviceLock:
            if state.isDeviceSecure {
                StepCard(
                    icon: "faceid",
                    title: String(localized: "Lock Offhand?"),
                    message: String(localized: "Offhand can ask for Face ID, Touch ID, or your passcode every time it opens, so your notes stay private even if someone else is holding an unlocked iPhone."),
                    content: {
                        VStack(spacing: 16) {
                            ToggleCard(
                                label: String(localized: "Require unlock to open Offhand"),
                                isOn: Binding(
                                    get: { state.isAppLockEnabled },
                                    set: { viewModel.onAppLockToggled(enabled: $0) }
                                )
                            )
                            Text(String(localized: "You can change this any time in Settings."))
                                .font(.footnote)
                                .foregroundStyle(Brand.onSurfaceVariant)
                                .multilineTextAlignment(.center)
                        }
                    }
                )
            } else {
                StepCard(
                    icon: "lock.slash",
                    title: String(localized: "Add a device passcode"),
                    message: String(localized: "This iPhone has no passcode, so Offhand has nothing to lock your notes with. Set one in Settings to use Face ID or Touch ID here.")
                )
            }
        case .telemetryConsent:
            StepCard(
                icon: "chart.bar.fill",
                title: String(localized: "Share usage & stability reports?"),
                message: String(localized: "Anonymous usage statistics and crash reports — things like app opens, device model and app version. Never your notes, audio, or any personal data."),
                content: {
                    ToggleCard(
                        label: String(localized: "Share usage & stability reports"),
                        isOn: Binding(
                            get: { state.isTelemetryEnabled },
                            set: { viewModel.onTelemetryToggled(granted: $0) }
                        )
                    )
                }
            )
        case .notifications:
            StepCard(
                icon: "bell.badge.fill",
                title: String(localized: "Know when notes are ready"),
                message: String(localized: "Notes keep preparing while Offhand is in the background. Allow notifications and Offhand will tell you the moment a note is ready to read.")
            )
        case .modelDownload:
            StepCard(
                icon: "arrow.down.circle.fill",
                title: String(localized: "Set up your private AI"),
                message: String(localized: "Offhand transcribes and summarizes voice notes with AI that runs entirely on your iPhone. To get started, it needs a one-time download of its AI models."),
                content: {
                    VStack(spacing: 16) {
                        DownloadSizeBadge(sizeGb: state.downloadSizeGb)
                        Text(String(localized: "The download continues in the background — you can start recording right away."))
                            .font(.footnote)
                            .foregroundStyle(Brand.onSurfaceVariant)
                            .multilineTextAlignment(.center)
                        Text(String(localized: "Tip: connect to Wi‑Fi to save mobile data."))
                            .font(.footnote)
                            .foregroundStyle(Brand.onSurfaceVariant)
                            .multilineTextAlignment(.center)
                    }
                }
            )
        default:
            EmptyView()
        }
    }

    private var continueTitle: String {
        if state.step == .deviceLock && !state.isDeviceSecure {
            return String(localized: "Continue without a lock")
        }
        return String(localized: "Continue")
    }

    private func continueAction(for step: OnboardingStep) -> (() -> Void)? {
        switch step {
        case .privacy:
            return { viewModel.onPrivacyContinue() }
        case .noteStyle:
            return { viewModel.onNoteStyleContinue() }
        case .deviceLock:
            return {
                guard state.isDeviceSecure && state.isAppLockEnabled else {
                    viewModel.onDeviceLockContinue()
                    return
                }
                // The first biometric check doubles as the system's Face ID
                // permission ask; running it here keeps that prompt on the card
                // that explains it, and proves the unlock works before the app
                // commits to locking. A cancelled check pulls a forward swipe
                // back to this card instead of advancing.
                DeviceAuthenticator.confirmOwner(
                    reason: String(localized: "Confirm it's you to turn on the app lock.")
                ) { confirmed in
                    if confirmed {
                        viewModel.onDeviceLockContinue()
                    } else {
                        withAnimation(.easeInOut(duration: 0.28)) {
                            visiblePage = Int(state.currentPage)
                        }
                    }
                }
            }
        case .telemetryConsent:
            return { viewModel.onConsentContinue() }
        case .notifications:
            return {
                NoteNotifications.shared.requestPermission {
                    viewModel.onNotificationsContinue()
                }
            }
        case .modelDownload:
            return {
                viewModel.onDownloadContinue()
                onFinished()
            }
        default:
            return nil
        }
    }

    private var incompatibleScreen: some View {
        StepCard(
            icon: "iphone.slash",
            iconTint: .orange,
            title: String(localized: "This device can't run Offhand"),
            message: String(localized: "Offhand needs more memory to run its on-device AI models."),
            content: {
                if let specs = state.deviceSpecs {
                    VStack(alignment: .leading, spacing: 12) {
                        specRow(
                            label: String(localized: "Memory"),
                            value: "\(specs.totalRamGb) GB / \(specs.requiredRamGb) GB",
                            satisfied: specs.isRamSatisfied
                        )
                        specRow(
                            label: String(localized: "CPU cores"),
                            value: "\(specs.cpuCores) / \(specs.requiredCpuCores)",
                            satisfied: specs.isCoresSatisfied
                        )
                    }
                    .padding()
                    .background(Brand.surface, in: RoundedRectangle(cornerRadius: 12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .strokeBorder(Color(.systemGray4), lineWidth: 1)
                    )
                }
            }
        )
        .padding(24)
    }

    private func specRow(label: String, value: String, satisfied: Bool) -> some View {
        HStack {
            Image(systemName: satisfied ? "checkmark.circle.fill" : "xmark.circle.fill")
                .foregroundStyle(satisfied ? Brand.primary : .red)
            Text(label)
            Spacer()
            Text(value).foregroundStyle(Brand.onSurfaceVariant)
        }
    }
}

private struct StepCard<Content: View>: View {
    let icon: String
    var iconTint: Color = Brand.primary
    let title: String
    let message: String
    @ViewBuilder var content: () -> Content

    init(
        icon: String,
        iconTint: Color = Brand.primary,
        title: String,
        message: String,
        @ViewBuilder content: @escaping () -> Content = { EmptyView() }
    ) {
        self.icon = icon
        self.iconTint = iconTint
        self.title = title
        self.message = message
        self.content = content
    }

    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 16) {
                    Image(systemName: icon)
                        .font(.system(size: 56))
                        .foregroundStyle(iconTint)
                        .padding(.bottom, 12)
                    Text(title)
                        .font(.title.bold())
                        .foregroundStyle(Brand.onSurface)
                        .multilineTextAlignment(.center)
                    Text(message)
                        .font(.body)
                        .foregroundStyle(Brand.onSurface)
                        .multilineTextAlignment(.center)
                    content()
                        .padding(.top, 8)
                }
                .padding(24)
                .frame(maxWidth: .infinity, minHeight: geometry.size.height)
            }
            .scrollBounceBehavior(.basedOnSize)
        }
        .background(Brand.surfaceContainer, in: RoundedRectangle(cornerRadius: 12))
    }
}

private struct PageDots: View {
    let currentPage: Int
    let pageCount: Int

    var body: some View {
        HStack(spacing: 8) {
            ForEach(0..<pageCount, id: \.self) { index in
                Capsule()
                    .fill(index == currentPage ? Brand.primary : Color(.systemGray4))
                    .frame(width: index == currentPage ? 24 : 8, height: 8)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: currentPage)
        .accessibilityLabel(
            String(
                format: String(localized: "Step %d of %d"),
                currentPage + 1,
                pageCount
            )
        )
    }
}

private struct ToggleCard: View {
    let label: String
    @Binding var isOn: Bool

    var body: some View {
        Toggle(label, isOn: $isOn)
            .tint(Brand.primary)
            .padding(.horizontal, 20)
            .padding(.vertical, 14)
            .background(Brand.surface, in: RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(Color(.systemGray4), lineWidth: 1)
            )
    }
}

private struct DownloadSizeBadge: View {
    let sizeGb: String

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: "arrow.down.circle")
                .font(.subheadline)
            Text(String(format: String(localized: "One-time download · about %@ GB"), sizeGb))
                .font(.subheadline.weight(.medium))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .foregroundStyle(Brand.onPrimaryContainer)
        .background(Brand.primaryContainer, in: Capsule())
    }
}

struct NotePresetPicker: View {
    let selected: NotePreset
    let onSelect: (NotePreset) -> Void

    private let options: [(NotePreset, String, String, String)] = [
        (
            .summary,
            String(localized: "Summary"),
            String(localized: "A clean write-up of what was said, without repetition or filler."),
            "doc.plaintext"
        ),
        (
            .meeting,
            String(localized: "Meeting notes"),
            String(localized: "Discussion, decisions, action items and open questions."),
            "person.3"
        ),
        (
            .visit,
            String(localized: "Visit report"),
            String(localized: "Who the visit was about, observations, what was done and follow-ups."),
            "list.clipboard"
        ),
        (
            .legal,
            String(localized: "Legal note"),
            String(localized: "Matter, facts stated, instructions, advice given and next steps."),
            "building.columns"
        ),
    ]

    var body: some View {
        VStack(spacing: 10) {
            ForEach(options, id: \.1) { option in
                presetCard(option)
            }
        }
        .animation(.easeInOut(duration: 0.15), value: selected)
    }

    private func presetCard(_ option: (NotePreset, String, String, String)) -> some View {
        let isSelected = selected == option.0
        return Button {
            onSelect(option.0)
        } label: {
            HStack(spacing: 16) {
                Image(systemName: option.3)
                    .foregroundStyle(Brand.primary)
                    .frame(width: 24)
                VStack(alignment: .leading, spacing: 2) {
                    Text(option.1)
                        .foregroundStyle(Brand.onSurface)
                    Text(option.2)
                        .font(.caption)
                        .foregroundStyle(Brand.onSurfaceVariant)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 12)
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundStyle(isSelected ? Brand.primary : Color(.systemGray3))
            }
            .padding(16)
            .background(
                isSelected ? Brand.primaryContainer : Brand.surface,
                in: RoundedRectangle(cornerRadius: 12)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(isSelected ? Brand.primary : Color(.systemGray4), lineWidth: 1)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
