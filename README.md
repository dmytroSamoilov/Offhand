# Offhand

[![Release](https://img.shields.io/github/v/release/dmytroSamoilov/Offhand?include_prereleases&label=release)](https://github.com/dmytroSamoilov/Offhand/releases)
![Status](https://img.shields.io/badge/status-stable-release)

> **Latest release: [Offhand 1.2.2](https://github.com/dmytroSamoilov/Offhand/releases/latest)** —
> Offhand comes to iPhone and iPad, six new languages, recording that survives
> switching apps, and the faster, more resilient processing engine from 1.2.0. See the
> [releases page](https://github.com/dmytroSamoilov/Offhand/releases) for the full history.

Private voice notes with on-device AI. Offhand records voice memos, meetings, and
dictations and turns them into structured, readable notes — **entirely on device**. No
audio, transcript, or note ever leaves your phone.

Record → on-device speech-to-text → on-device LLM structures the transcript into a
Markdown note with an AI-generated title → stored encrypted → read, edit, share, delete.

Built for anyone whose spoken thoughts are nobody else's business: executives capturing
meeting debriefs, doctors dictating patient notes, consultants and lawyers with
confidentiality obligations — or just your own ideas on a walk.

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
  (headings, lists, action items) — not a lossy summary. Long recordings are structured in
  ~2,500-token segments so they fit the model's 4,096-token context window.
- **Encryption**: notes live in a SQLCipher-encrypted Room database. The passphrase is
  random, wrapped by an AES-256-GCM key in the Android Keystore, and — on devices with a
  lockscreen — bound to user authentication (fingerprint or device credential via
  `BiometricPrompt`).
- **Adaptive UI**: Jetpack Compose + Material 3 adaptive components. Bottom bar on phones,
  navigation rail and list-detail two-pane on tablets and foldables.

## Verify the privacy claims yourself

That is the point of this repo being public:

- **Network access** — the only outbound traffic in the codebase is the one-time model
  download over HTTPS:
  [`ModelDownloader.kt`](core/ai-local/src/main/kotlin/com/dmytrosamoilov/offhand/core/ai/local/ModelDownloader.kt).
- **Telemetry is off by default and opt-in** — Crashlytics and Analytics auto-collection
  are disabled in the [manifest](app/src/main/AndroidManifest.xml) and only activated when
  you consent:
  [`TelemetryController.kt`](app/src/main/java/com/dmytrosamoilov/offhand/telemetry/TelemetryController.kt).
  Crash reports and usage statistics never contain note content.
- **Encryption at rest** — SQLCipher database and Keystore-wrapped keys in
  [`core/security`](core/security) and [`core/data`](core/data).
- **No screenshots, no backups** — `FLAG_SECURE` blocks screen capture;
  `allowBackup="false"` keeps notes out of cloud backups.

## Requirements

- Android 12+ (minSdk 31), **5+ GB RAM and 4+ CPU cores** (checked at first launch)
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
9. Custom note styles: create one with two sections, preview it on the sample, pick it as the default, rewrite a note with it, delete it
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
:core:security          Keystore passphrase wrapping, app lock
:core:data              encrypted Room notes + DataStore preferences
:feature:onboarding     device check → model download → consent
:feature:recording      recording UI, foreground service, AI pipeline
:feature:notes          list / detail / edit, adaptive two-pane
:feature:settings       acceleration tier, model management, privacy
:feature:backup         passphrase-encrypted backup and restore of notes, folders and audio
:testing:fakes          canned AI, model, device and microphone for smoke tests
```

Three layers inside each feature (domain → data → presentation), use cases wrapping
repositories, StateFlow-only ViewModels, mappers between domain and UI models.

## License

The source code is licensed under [GPL-3.0](LICENSE) © Dmytro Samoilov. Third-party
components and AI models are listed in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

Offhand is open source but **not open contribution** — pull requests are not accepted;
issues and bug reports are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for why.

The **Offhand** name and logo are not covered by the code license and may not be used for
derivative apps or forks without permission.

Legal documents for the published app:
[Privacy Policy](legal/privacy-policy.html) · [Terms & Conditions](legal/terms-and-conditions.html)
