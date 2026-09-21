# Offhand

[![Release](https://img.shields.io/github/v/release/dmytroSamoilov/Offhand?include_prereleases&label=release)](https://github.com/dmytroSamoilov/Offhand/releases)
![Status](https://img.shields.io/badge/status-stable-release)

<p><a href="https://apps.apple.com/app/offhand-private-voice-notes/id6803641617"><img src="https://toolbox.marketingtools.apple.com/api/badges/download-on-the-app-store/black/en-us?size=250x83" alt="Download on the App Store" height="54" align="middle"></a>&nbsp;&nbsp;<a href="https://play.google.com/store/apps/details?id=com.dmytrosamoilov.offhand&hl=en"><img src="https://play.google/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="72" align="middle"></a></p>

> **Latest release: [Offhand 1.3.0](https://github.com/dmytroSamoilov/Offhand/releases/latest)** —
> search and folders, encrypted backup & restore with the recordings, transcriptions
> that continue where they stopped, a better Summary style, and the first paid tier,
> Offhand Pro: your own note styles, PDF and Word export, smart calendar suggestions and
> audio file import. See [What's new in 1.3](#whats-new-in-13) and the
> [releases page](https://github.com/dmytroSamoilov/Offhand/releases) for the full history.

Private voice notes with on-device AI, for Android and iPhone. Offhand records voice
memos, meetings, and dictations and turns them into structured, readable notes —
**entirely on device**. No audio, transcript, or note ever leaves your phone.

Record → on-device speech-to-text → on-device LLM structures the transcript into a
Markdown note with an AI-generated title → stored encrypted → read, edit, share, delete.

Built for anyone whose spoken thoughts are nobody else's business: executives capturing
meeting debriefs, doctors dictating patient notes, consultants and lawyers with
confidentiality obligations — or just your own ideas on a walk.

## What's new in 1.3

- **Search and folders** — full-text search across titles, overviews and transcripts with
  highlighted snippets; notes can be filed into folders, moved from the menu or with a
  swipe, and filtered by folder.
- **Backup & restore** — one passphrase-encrypted `.offhand` file with notes, folders,
  custom styles and the original recordings (AES-256-GCM, PBKDF2-derived key). Restore
  merges into the existing library and skips duplicates.
- **Copy any section** — Overview and Transcript each have their own copy button.
- **Better Summary notes** — the default style now produces main topics, key decisions,
  action items and an overview instead of a prose recap. Every style is sectioned.
- **Transcription that survives interruptions** — progress is checkpointed after every
  window, so a note killed by the system (or a failed run) continues from where it
  stopped instead of starting over. iOS finishes notes in the background with
  `BGContinuedProcessingTask` on iOS 26.
- **App lock** — biometric or device-credential lock, offered during onboarding and in
  Settings; an active recording carries across app switches without re-locking.
- **Offhand Pro** — the first paid tier, yearly with a 14-day free trial or a one-time
  lifetime purchase. Pro unlocks:
  - **Note styles** — your own styles built from headings, per-section guidance and a
    format (short, bullets, free), or described in plain words and drafted by the
    on-device model. Styles are forms, not free prompts, so the output shape stays fixed.
  - **PDF and Word export** — a note goes out as PDF or DOCX, written on device, next to
    the existing text and audio sharing.
  - **Smart suggestions** — the model finds dates and meetings in a note and offers
    them as calendar events; you review each one in the system calendar editor before
    anything is saved. Off by default.
  - **Audio import** — pick audio files (up to two hours each) or share them into the
    app; each becomes a note through the same on-device pipeline.
  - Everything stays visible and tappable while free; the paywall opens on the action,
    never unprompted. Payments go through Google Play and the App Store directly — no
    payment SDK, no account, no server.

## How it works

- **Speech-to-text**: [Whisper small (int8)](https://huggingface.co/csukuangfj/sherpa-onnx-whisper-small)
  running locally via [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx). Audio is
  transcribed in chunks *while the recording continues*.
- **Note structuring**: [Gemma 4 E2B](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm)
  (2B effective parameters, Apache-2.0) running on device via
  [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM), with a Tensor
  G5-optimized build selectable on Pixel 10 devices.
- **Recording**: 16 kHz PCM16 with RMS-based voice-activity detection, chunked at natural
  silence boundaries inside a `microphone` foreground service.
- **Structuring**: the merged transcript is rewritten into detail-preserving Markdown
  sections (headings, lists, action items) according to the chosen note style — not a
  lossy summary. Long recordings are structured in segments sized to the model's
  4,096-token context window and merged afterwards, with duplicate statements removed.
  Small on-device models truncate, miscount and bend JSON, so every step has a parser
  that recovers what it can and a fallback that never loses the transcript.
- **Checkpoints**: transcription progress is stored after every 29-second window; an
  interrupted note resumes from the last checkpoint on the next launch or background run.
- **Encryption on Android**: notes live in a SQLCipher-encrypted Room database. The
  passphrase is random, wrapped by an AES-256-GCM key in the Android Keystore, and — on
  devices with a lockscreen — bound to user authentication (fingerprint or device
  credential via `BiometricPrompt`). Recordings are stored through Tink streaming AEAD.
- **Encryption on iOS**: the database and the recordings use the system's
  complete-unless-open data protection (`NSFileProtectionCompleteUnlessOpen`), so they
  are unreadable while the device is locked and never leave the app sandbox; backups
  use CryptoKit with the same file format as Android.
- **UI**: Jetpack Compose + Material 3 adaptive components on Android (bottom bar on
  phones, navigation rail and list-detail two-pane on tablets and foldables); SwiftUI on
  iOS. The domain, data and ViewModel layers are shared Kotlin Multiplatform code, so
  both apps run the same pipeline, parsers and business rules.

## Verify the privacy claims yourself

That is the point of this repo being public:

- **Network access** — the only outbound traffic in the codebase is the one-time model
  download over HTTPS:
  [`ModelDownloader.kt`](core/ai-local/src/main/kotlin/com/dmytrosamoilov/offhand/core/ai/local/ModelDownloader.kt)
  on Android and
  [`IosFileDownloader.kt`](shared-framework/src/iosMain/kotlin/com/dmytrosamoilov/offhand/shared/IosFileDownloader.kt)
  on iOS.
- **Telemetry is off by default and opt-in** — Firebase never initialises on its own:
  the provider is removed from the [manifest](app/src/main/AndroidManifest.xml) and
  Crashlytics and Analytics are only configured after you consent, in
  [`TelemetryController.kt`](app/src/main/java/com/dmytrosamoilov/offhand/telemetry/TelemetryController.kt)
  and [`TelemetryController.swift`](iosApp/Offhand/Telemetry/TelemetryController.swift).
  Crash reports and usage statistics never contain note content, titles, folder names or
  search queries; the full list of events is in
  [`AnalyticsEvents.kt`](core/data/src/commonMain/kotlin/com/dmytrosamoilov/offhand/core/data/domain/analytics/AnalyticsEvents.kt).
- **Encryption at rest** — SQLCipher database and Keystore-wrapped keys in
  [`core/security`](core/security) and [`core/data`](core/data); file protection on iOS
  in the same modules' `iosMain` sources.
- **Backups are yours** — `allowBackup="false"` keeps notes out of Android cloud
  backups; the only backup is the passphrase-encrypted file you export yourself
  ([`feature/backup`](feature/backup)). Screenshots and screen recording are allowed.
- **Payments stay with the stores** — Play Billing and StoreKit 2 are called directly
  ([`PlayProStore.kt`](core/data/src/androidMain/kotlin/com/dmytrosamoilov/offhand/core/data/billing/PlayProStore.kt),
  [`StoreKitProStore.swift`](iosApp/Offhand/Store/StoreKitProStore.swift)); there is no
  third-party billing SDK, no account and no server that learns what you bought.

## Requirements

- Android 12+ (minSdk 31), **5+ GB RAM and 4+ CPU cores** (checked at first launch)
- iPhone or iPad on iOS 18 or later
- ~3 GB free storage for the one-time model downloads (Whisper + Gemma)

## Building

Three product flavors that install side by side: `production` (`com.dmytrosamoilov.offhand`),
`dev` (`com.dmytrosamoilov.offhand.dev`, "Offhand Dev" label) and `uitest`
(`com.dmytrosamoilov.offhand.uitest`, "Offhand UI Test" — see [Testing](#testing)).

1. Clone and open in Android Studio (or use `./gradlew assembleDevDebug`). The models
   (Whisper + Gemma) are ungated on Hugging Face — no account or token needed; the app
   downloads them on first run.
2. Optional — Firebase telemetry (opt-in crash reporting + usage analytics) needs your own
   Firebase project's `google-services.json` in `app/`. The app builds and runs fine
   without it.

```
./gradlew assembleDebug testDebugUnitTest lintDebug :app:lintDevDebug
```

### iOS

The iOS app lives in `iosApp/` and links the shared Kotlin code as an XCFramework.
Schemes mirror the Android flavors: `Offhand-dev`, `Offhand-prod` and `Offhand-uitest`.

1. Install [XcodeGen](https://github.com/yonaskolb/XcodeGen) and Git LFS, then build the
   shared framework and fetch the speech-to-text frameworks:

   ```
   ./gradlew :shared-framework:assembleOffhandSharedReleaseXCFramework
   iosApp/Tools/fetch-frameworks.sh
   ```

2. Generate the project and build:

   ```
   cd iosApp && xcodegen generate
   GIT_LFS_SKIP_SMUDGE=1 xcodebuild build -project Offhand.xcodeproj -scheme Offhand-dev \
     -destination 'generic/platform=iOS Simulator' ARCHS=arm64
   ```

   `GIT_LFS_SKIP_SMUDGE=1` is needed for a fresh package checkout: LiteRT-LM keeps
   prebuilt libraries in Git LFS next to its sources and the iOS binary comes from a
   release archive instead, so the LFS files are left as pointers.

3. Optional — Firebase telemetry needs your own `GoogleService-Info.plist` under
   `iosApp/Firebase/dev/` or `iosApp/Firebase/prod/`; without it the build warns and
   telemetry stays off.

## Testing

### Unit tests

```
./gradlew testDebugUnitTest
```

Tests that need no Android or MockK live in `commonTest` and also run on the
Kotlin/Native iOS target, which catches K/N-only differences (regex classes, stdlib)
that the JVM run cannot:

```
./gradlew :core:ai-api:iosSimulatorArm64Test :core:audio:iosSimulatorArm64Test \
  :core:common:iosSimulatorArm64Test :core:device:iosSimulatorArm64Test \
  :feature:notes:iosSimulatorArm64Test :feature:recording:iosSimulatorArm64Test
```

### Smoke tests (Maestro)

The `uitest` flavor (Android) and the `Offhand-uitest` scheme (iOS) are the normal app
with the AI swapped for fakes from `:testing:fakes`: the model manager reports the model
as downloaded, speech-to-text returns canned sentences, the note structurer returns a
fixed note titled "Smoke test note", and the microphone is a tone generator. So the
whole record → note flow runs on any emulator or simulator, with no 2.4 GB download,
no real audio, and no device gate. Nothing else is faked — onboarding, the encrypted
database, the recording pipeline, list, search and detail are all real.

The [Maestro](https://maestro.mobile.dev) flows in `.maestro/` drive both platforms
and cover:

1. Fresh install and onboarding (app lock and telemetry consent switched off)
2. Recording through the fake microphone, saving, closing the sheet
3. The processed note appearing in the list
4. Search for "budget" with the highlighted snippet
5. Opening the note and rendering the Overview and Transcript sections
6. Folders: create, move the note in (menu and leading swipe), filter by folder, rename, delete
7. Returning from a note leaves the search field unfocused (Android)
8. Backup and restore through the system file picker, including the passphrase prompt (Android only; the iOS picker is not scriptable)
9. Custom note styles: create one with two sections, pick it as the default, rewrite a note with it, delete it
10. Building a note style from a plain-text description; the fake model answers the drafting prompt with a canned two-section style
11. Importing an audio file from Settings through the system picker into a new note (needs `.maestro/assets/import-sample.m4a` in the device's Download folder on Android or in the simulator's "On My iPhone" storage on iOS, see below)
12. Smart suggestions: switching them on in Settings, asking for them on the finished note; the fake model answers with two canned calendar events, one is dismissed, the other is handed to the system calendar's event editor, and both states survive a relaunch
13. The Pro paywall: the debug override in Settings plays a free user, crowns appear, the paywall opens from the Upgrade card and from Import audio, a simulated purchase unlocks the app and the import picker that was waiting behind the paywall opens on its own

The flows assume a phone-sized screen; on tablets and unfolded foldables the app switches
to its two-pane layout and the steps no longer line up.

Playback, editing, sharing, deleting, settings and model quality are not covered.

**Install Maestro** (the plain `maestro` formula is an unrelated app — use the tap):

```
brew trust mobile-dev-inc/tap && brew install mobile-dev-inc/tap/maestro
```

**Android**, with an emulator running or a device connected:

```
./gradlew assembleUitestDebug
adb install -r app/build/outputs/apk/uitest/debug/app-uitest-debug.apk
adb push .maestro/assets/import-sample.m4a /sdcard/Download/import-sample.m4a
maestro test .maestro/
```

**iOS**, with a booted simulator: build the `Offhand-uitest` scheme (Xcode, or
`xcodebuild -scheme Offhand-uitest -destination 'id=<simulator udid>'`), install the
`.app` with `xcrun simctl install booted <path>`, then:

```
xcrun simctl privacy booted grant microphone com.dmytrosamoilov.offhand.uitest
maestro test .maestro/
```

With several devices connected, add `--device <id>` before `test`. A failed run leaves
screenshots and a UI hierarchy dump under `~/.maestro/tests/<timestamp>/`.

Two platform quirks the flow already handles: the Android record sheet starts recording
by itself when opened, and iOS toggles only react to taps on the switch, so the
onboarding toggle cards accept a tap anywhere on the row.

### Continuous integration

`.github/workflows/ci.yml` runs on every push and pull request: Android build, unit
tests and lint; the shared tests on the iOS simulator target; and the smoke flow on an
Android emulator (Ubuntu, KVM) and on an iOS simulator (macOS). Maestro artifacts are
attached to failed runs.

## Architecture

Multi-module, convention-plugin based. Features depend on abstractions, never
implementations:

```
:app                    thin shell: activity, navigation, DI bindings
:core:common            BaseViewModel, shared primitives
:core:designsystem      Material 3 theme + reusable components
:core:ui                screen scaffolding (loading/error handling)
:core:device            device capability gate (RAM / CPU cores)
:core:audio             AudioRecord streaming + VAD chunking
:core:ai-api            AI abstractions — no LiteRT dependency
:core:ai-local          LiteRT-LM engine, Whisper STT, model download, catalog
:core:security          Keystore passphrase wrapping, encrypted audio store, app lock
:core:data              encrypted Room notes, folders, checkpoints, Pro status, billing, analytics events
:feature:onboarding     device check → model download → consent
:feature:recording      recording UI, foreground service, AI pipeline, note styles, calendar suggestions
:feature:notes          list / search / folders / detail / edit, export to text, PDF and DOCX
:feature:settings       note styles, default style, backup, import, subscription, privacy
:feature:backup         passphrase-encrypted backup and restore of notes, folders, styles and audio
:feature:paywall        Offhand Pro paywall and purchase flow
:shared-framework       Kotlin Multiplatform framework consumed by the iOS app
:testing:fakes          canned AI, model, device, microphone and store for smoke tests
```

Three layers inside each feature (domain → data → presentation), use cases wrapping
repositories, StateFlow-only ViewModels, mappers between domain and UI models. Feature
and core modules are Kotlin Multiplatform: `commonMain` holds the logic, `androidMain`
the Compose UI and Android services, `iosMain` the platform bridges the SwiftUI app
calls through `shared-framework`.

## License

The source code is licensed under [GPL-3.0](LICENSE) © Dmytro Samoilov. Third-party
components and AI models are listed in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

Offhand is open source but **not open contribution** — pull requests are not accepted;
issues and bug reports are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for why.

The **Offhand** name and logo are not covered by the code license and may not be used for
derivative apps or forks without permission.

Legal documents for the published app:
[Privacy Policy](legal/privacy-policy.html) · [Terms & Conditions](legal/terms-and-conditions.html)
