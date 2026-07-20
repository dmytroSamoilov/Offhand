# Offhand

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
- **Crash reporting is off by default and opt-in** — Crashlytics auto-collection is
  disabled in the [manifest](app/src/main/AndroidManifest.xml) and only activated when you
  consent:
  [`CrashReportingController.kt`](app/src/main/java/com/dmytrosamoilov/offhand/telemetry/CrashReportingController.kt).
  Crash reports never contain note content.
- **Encryption at rest** — SQLCipher database and Keystore-wrapped keys in
  [`core/security`](core/security) and [`core/data`](core/data).
- **No screenshots, no backups** — `FLAG_SECURE` blocks screen capture;
  `allowBackup="false"` keeps notes out of cloud backups.

## Requirements

- Android 12+ (minSdk 31), **5+ GB RAM and 4+ CPU cores** (checked at first launch)
- ~3 GB free storage for the one-time model downloads (Whisper + Gemma)

## Building

Two product flavors: `production` (`com.dmytrosamoilov.offhand`) and `dev`
(`com.dmytrosamoilov.offhand.dev`, "Offhand Dev" label) — they install side by side.

1. Clone and open in Android Studio (or use `./gradlew assembleDevDebug`). The models
   (Whisper + Gemma) are ungated on Hugging Face — no account or token needed; the app
   downloads them on first run.
2. Optional — Crashlytics (opt-in telemetry) needs your own Firebase project's
   `google-services.json` in `app/`. The app builds and runs fine without it.

```
./gradlew assembleDebug testDebugUnitTest lintDebug :app:lintDevDebug
```

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
```

Three layers inside each feature (domain → data → presentation), use cases wrapping
repositories, StateFlow-only ViewModels, mappers between domain and UI models.

## License

The source code is licensed under [GPL-3.0](LICENSE) © Dmytro Samoilov. Third-party
components and AI models are listed in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

The **Offhand** name and logo are not covered by the code license and may not be used for
derivative apps or forks without permission.

Legal documents for the published app:
[Privacy Policy](legal/privacy-policy.html) · [Terms & Conditions](legal/terms-and-conditions.html)
