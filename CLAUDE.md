# Claude Code Instructions
<!-- Version: 2 -->
Version: v2

## Project
This is Offhand — a privacy-first Android app that records voice notes and meetings and turns them into structured notes entirely on device.
See .claude/PROJECT_SPEC.md for full project overview, tech stack, and architecture.
POC decisions and roadmap: .claude/POC/ (read before implementing features).

## Behavior
- If you know a better solution (different API, library, pattern) — suggest it BEFORE implementing. Don't silently use a suboptimal approach.
- After every code change, run `./gradlew assembleDebug` and fix errors before finishing.
- When adding new files, auto `git add` them (only files related to current task).

## Build & Test

### Android
- Flavors: `production` (com.dmytrosamoilov.offhand) and `dev` (.dev applicationId suffix, "Offhand Dev" label). Day-to-day work and device installs use `dev`.
- Smoke-test flavor `uitest` (.uitest suffix, "Offhand UI Test"): `app/src/uitest` installs `smokeFakesModule` from `:testing:fakes`, replacing the AI engines, model manager, device gate and microphone with canned fakes, so the whole record → note flow runs on an emulator without the 2.4 GB model. The Google Services task is disabled for it. Build with `./gradlew assembleUitestDebug`; the Maestro flow lives in `.maestro/`.
- Build: `./gradlew assembleDevDebug` (both flavors: `assembleDebug`)
- Unit tests: `./gradlew testDebugUnitTest`
- Shared tests on Kotlin/Native (catches K/N-only regex and stdlib differences): `./gradlew :core:ai-api:iosSimulatorArm64Test :core:audio:iosSimulatorArm64Test :core:common:iosSimulatorArm64Test :core:device:iosSimulatorArm64Test :feature:notes:iosSimulatorArm64Test :feature:recording:iosSimulatorArm64Test`. Put MockK-free tests in `commonTest` with `kotlin.test`; K/N rejects commas in backticked test names and JUnit's message-first assert order.
- Lint: `./gradlew lintDebug :app:lintDevDebug`
- Run all: `./gradlew assembleDebug testDebugUnitTest lintDebug :app:lintDevDebug`
- Telemetry: `FirebaseInitProvider` is removed in `AndroidManifest.xml`, so Firebase never self-initialises — `TelemetryController` initialises it (Crashlytics + Analytics) only after telemetry consent, because initialising alone contacts Google. Keep it that way, and keep `ReleaseLogTree` resolving Crashlytics per log rather than capturing it, since Firebase may not exist yet when the tree is planted. iOS holds the same line.

### iOS
- Flavors mirror Android via Xcode configurations: schemes `Offhand-dev` (com.dmytrosamoilov.offhand.dev, "Offhand Dev") and `Offhand-prod` (com.dmytrosamoilov.offhand, "Offhand"). Day-to-day work uses `Offhand-dev`.
- Smoke-test scheme `Offhand-uitest` (config `Debug-uitest`, .uitest suffix) compiles with `UI_TEST`, which makes `OffhandApp` start Koin with the same `smokeFakesModule`. It has no Firebase config and no release configuration.
- Configurations: `Debug-dev`, `Release-dev`, `Debug-prod`, `Release-prod`. The flavor is driven by `APP_ID_SUFFIX` / `APP_DISPLAY_NAME` in `iosApp/project.yml`.
- `iosApp/Offhand.xcodeproj` is generated and gitignored — edit `iosApp/project.yml`, then run `xcodegen generate` in `iosApp/`. Close the project in Xcode first: regenerating under an open Xcode leaves it holding a stale project, and its saved scheme selection in `xcuserdata` can point at a scheme that no longer exists. If Xcode misbehaves after a regenerate, quit it and run `rm -rf Offhand.xcodeproj/xcuserdata Offhand.xcodeproj/project.xcworkspace/xcuserdata` — that is editor state only, and `Package.resolved` lives in `xcshareddata` so SPM pins survive.
- Shared framework (rebuild after any commonMain/iosMain change): `./gradlew :shared-framework:assembleOffhandSharedReleaseXCFramework`
- Build: `cd iosApp && xcodebuild build -project Offhand.xcodeproj -scheme Offhand-dev -destination 'generic/platform=iOS Simulator' ARCHS=arm64`
- Only arm64 slices are built for the simulator, so pass `ARCHS=arm64` (an unrestricted `generic/platform=iOS Simulator` build fails on the x86_64 slice).
- Telemetry: put the per-flavor Firebase configs at `iosApp/Firebase/dev/GoogleService-Info.plist` and `iosApp/Firebase/prod/GoogleService-Info.plist` (gitignored, same as Android's `google-services.json`). A build phase copies the one matching `FIREBASE_CONFIG_DIR`; a missing file warns and leaves telemetry off rather than failing. Firebase (Crashlytics + Analytics, Swift `TelemetryController`) is only configured once telemetry consent is granted, so an app without consent makes no network calls at all — do not move `FirebaseApp.configure()` to launch.
- App icons are derived from the Android launcher vectors (`app/src/main/res/drawable/ic_launcher_*.xml`). If that art changes, re-run `python3 iosApp/Tools/make-app-icon.py iosApp/Offhand/Assets.xcassets`. `AppIcon` is the prod (blue) mark, `AppIcon-dev` the teal dev variant; the flavor picks one via `APP_ICON_NAME`.
- App lock: enforced in every configuration, dev and debug included — there is no build-time escape hatch, and the removed `DEV_UNLOCK` flag must not come back. To work without the lock, decline it in onboarding or turn it off in Settings; that is the same path a user has. `NSFaceIDUsageDescription` is mandatory: without it iOS refuses Face ID outright and `.deviceOwnerAuthentication` silently degrades to a passcode prompt, which looks like "biometrics just never fire". The lock is gated on the `app_lock_enabled` preference the onboarding step writes — `AppLockManager` only reports lock state, it does not decide whether to lock. Re-locking on background is driven by the app layer, which can see the recording session (the managers cannot, for module layering): `OffhandApplication` observes `ProcessLifecycleOwner` and `RootView.handleBackgrounded()` observes `scenePhase`, and each calls `markLocked()` — but skips it while `SessionPhase.RECORDING`, because swapping in the lock screen tears down the record sheet and strands the live capture. So an unlock lasts one foreground session, except an active recording carries across background/foreground. `markLocked()` is a no-op while the device has no credential (`isDeviceSecure == false`) on both platforms: the stored preference can be stale (pre-opt-in installs default it to on, and removing the device PIN does not rewrite it), and a lock screen with nothing to authenticate against is a dead end. Compose `takeFocus` on window-focus regain lands on the first focusable node, which on the notes list is the search field; `Modifier.userInitiatedFocusOnly()` (core:designsystem) cancels focus entering the pane unless a touch was seen within the last second, so taps still focus and show the keyboard but returning from a screen does not. (A pointer-down flag reset on the Final pass did not work: the field's focus request runs after that reset.)
- Testing the lock on the Simulator: enroll biometrics with `xcrun simctl spawn <udid> notifyutil -s com.apple.BiometricKit.enrollmentChanged 1` then `notifyutil -p com.apple.BiometricKit.enrollmentChanged`, and satisfy a prompt with `notifyutil -p com.apple.BiometricKit_Sim.pearl.match`. Note that an instantaneous synthetic tap does not flip a SwiftUI `Toggle` — drive it with a touch path that dwells ~150 ms.

## Backup format
- `:feature:backup` writes a single `.offhand` file: 4-byte magic `OFHB`, version byte, PBKDF2 iteration count, 16-byte salt, 8-byte nonce prefix, then a stream of chunks (`flag | u32 length | AES-256-GCM ciphertext`), 1 MiB of plaintext per chunk, nonce = prefix + counter, AAD = magic + counter + final flag. Inside the plaintext: length-prefixed records (manifest JSON, folders JSON, notes JSON, one raw PCM record per audio file, END). The format and both use cases live in commonMain over okio; only `BackupCrypto` is per platform (javax.crypto on Android, CryptoKit + CommonCrypto in Swift via `IosBackupCryptoBridge`). Restore merges: folders match by name (case-insensitive), a note is a duplicate when title and createdAt match, in-flight notes are never backed up. Bump `BackupFormat.VERSION` for any layout change and keep old readers. Android's `TinkEncryptedAudioStore.pcmSizeOf` must read a byte before calling `size()`: Tink's keyset-wrapped seekable channel throws until the first read, and callers wrap the call in `runCatching`, which silently turned every recording into 0 bytes and dropped audio from backups. The backup KDF on Android is the hand-rolled `Pbkdf2HmacSha256` over the raw UTF-8 passphrase bytes, because `PBKDF2WithHmacSHA256` re-encodes chars and would not match iOS's CommonCrypto for non-ASCII passphrases.

## Note styles
- A note's style is a `NoteStyleRef`: `BuiltIn(NotePreset)` or `Custom(id)`, stored as the legacy `preset` column plus a nullable `customStyleId` (DB v8, `note_styles` table with the sections as JSON). The same ref is the DataStore default (`storageKey()`: preset name or `custom:<id>`) and the service intent extra. Deleting a custom style nulls `customStyleId` on its notes and resets the default preference, so those notes fall back to Summary.
- Prompts are built from a `NoteStyleSpec` (feature:recording): `BuiltInNoteStyles` holds Meeting, Visit and Legal as hand-written specs and composes Summary from four `NoteStyleSection`s (Main Topics, Key Decisions, Action Items as bullets, Summary Overview as sentences; kind "a summary of the recording") through the same builder custom styles use, because that form, drafted by the owner with the describe tool, produced clearly better notes than the old first-person prose summary. Every style is sectioned now; the prose formatter and its polish rule are gone. `CustomNoteStyleSpecBuilder` composes a custom style from headings, per-section guidance and format into the same rule templates, and `NoteStyleResolver` maps a ref to a spec (a missing custom id resolves to Summary and the note is stored with that ref). Custom styles are forms, not free prompts: user text is dropped into quoted slots after stripping `"{}\\`<>` and whitespace runs, so the JSON shape, factuality and section-merge rules stay fixed. Keep it that way; do not add a free-text "instructions" field without a hard cap and the same sanitising.
- "Describe it and let the AI fill the form" (`NoteStyleDrafter`, `SessionNoteStyleDrafter`) sends the user's description with `NoteStyleDraftPrompt` (always with a thinking block, independent of the polish `THINKING_ENABLED` flag) and `NoteStyleDraftParser` turns the JSON into a `NoteStyleDraft` that fills the editor; an unparsable answer throws `NoteStyleDraftException`, a missing AI core returns null. The uitest `FakeAiBackend` recognises the drafting prompt by its first sentence and answers with a canned style, so keep that sentence stable or update the fake.
- "Try it on a sample" (`NoteStylePreviewer`, implemented by `SessionNoteStylePreviewer` in feature:recording over `RecordingSessionManager.previewNoteStyle`) runs the unsaved style through the real structurer under the processing mutex on a localised sample transcript; it returns null when the AI core is not downloaded. Custom styles are a paid feature behind the `EntitlementsRepository` seam in core:data, currently `MockEntitlementsRepository` (flip its constant to test the locked state) until billing lands. Every entry point goes through an `IsCustomNoteStylesAvailableUseCase` (one per feature): locked hides the New button, blanks the editor and its Save action, drops custom styles from the default picker and the restyle sheet, and `GetNoteStyleUseCase` falls back to Summary for a custom default, so a lapsed purchase never produces a custom-styled note. Existing custom styles and notes are kept, not deleted. `NotesViewModel` has more than 22 constructor parameters, so it is registered with an explicit `viewModel { }` block, not `viewModelOf`.
- Backup format v2 adds a `STYLES` record before `FOLDERS` and `customStyleId` on notes; restore matches styles by name (case-insensitive) and remaps ids. v1 files still open; v2 files do not open in 1.2.x.
- Maestro on Android does not see the label of an `ExtendedFloatingActionButton`; give it a `semantics { contentDescription }`.
- iOS: `NoteStylesView` and `NoteStyleEditorView` are SwiftUI mirrors of the Android screens. Per-screen ViewModels with parameters come from `SharedGraph.noteStyleEditor(styleId:)`, which returns a `NoteStyleEditorHandle` wrapping the ViewModel in its own `ViewModelStore`; the view creates it on first appearance (never in `init`, SwiftUI rebuilds structs and NavigationLink destinations eagerly) and calls `close()` in `onDisappear` so the coroutines stop. Value-based `NavigationLink`/`navigationDestination` popped the editor straight back in this stack; use the view-based link like the rest of the app. Forms with several text fields need the keyboard `Done` toolbar button plus `.scrollDismissesKeyboard(.immediately)`, otherwise Maestro's next tap lands on the keyboard; a filled SwiftUI `TextField` still matches its placeholder text, so index into repeated fields. The Settings form has a "Notes" section header, so Maestro must tap the tab with `index: 1`.

## Architecture (summary)
- Three layers: domain/ → data/ → presentation/
- Use cases wrap repository calls. When adding a new use case, refactor all direct repo calls to use it.
- ViewModels expose UI state via StateFlow. No LiveData.
- Each screen: single UiState data class (content only — no isLoading/error, BaseViewModel handles those).
- UiState uses UI-specific models, not domain entities. Use mappers.
- All screens wrapped with BaseComposeScreen. All ViewModels extend BaseViewModel.
- Full architecture rules: .claude/PROJECT_SPEC.md#architecture

## Code Style
- SOLID principles — strictly enforced
- Composition over inheritance
- Functions: single responsibility, <20 lines
- No code comments — self-documenting through naming
- No dead code, unused variables, or speculative code
- Explicit imports only (no wildcards)
- Strings go in strings.xml — no hardcoded, no duplicates

## Testing
- Unit tests mandatory for all use cases with domain logic
- Pattern: Arrange-Act-Assert
- Mock external dependencies
- Tests: isolated, repeatable, fast

## Resources
- String names must have entries in strings.xml
- No duplicate string resources

## Security (MANDATORY)
Every solution you implement MUST be secure by default. Before finishing any change, mentally walk through this checklist against the code you wrote. If a rule conflicts with convenience, the rule wins — raise the concern, do not silently weaken security.

### Secrets & sensitive data
- NEVER hardcode API keys, tokens, passwords, signing keys, URLs of private endpoints, or any secret in source, `BuildConfig`, `strings.xml`, or comments. Load from `local.properties` (gitignored) → `BuildConfig` at build time, or from a secure backend.
- NEVER commit `.env`, `google-services.json` for prod, keystores, or `local.properties`. Verify `.gitignore` covers them before `git add`.
- NEVER log tokens, passwords, PII, full request/response bodies, auth headers, or device identifiers. `Timber`/`Log` in release builds must be stripped via ProGuard/R8 or guarded by `BuildConfig.DEBUG`.
- Passwords and tokens in memory: prefer `CharArray`/`ByteArray` over `String` and zero them after use. `String` is immutable and sits in the heap until GC.
- Sensitive data in `SharedPreferences` → use `EncryptedSharedPreferences` (Jetpack Security) or `DataStore` with encryption. Plaintext `SharedPreferences` is forbidden for anything sensitive.
- Tokens on disk → Android Keystore-backed keys only. Never store private keys in files or prefs.
- Do not put sensitive data in `Intent` extras that cross app boundaries, clipboard, notifications, or URL query strings.
- Screens showing sensitive data must set `FLAG_SECURE` to block screenshots and recents thumbnails.

### Networking
- HTTPS only. `android:usesCleartextTraffic="false"` and a restrictive `network_security_config.xml`. Never add a custom `TrustManager`/`HostnameVerifier` that accepts all certs — if SSL fails, fix the cert, don't bypass.
- Pin certificates for production APIs where feasible (OkHttp `CertificatePinner`).
- `OkHttp` logging interceptor: `Level.BODY` only in debug. Auth headers must be redacted via `redactHeader(...)`.
- Validate every server response before use. Never `!!` on network data. Treat all remote input as untrusted.
- Use Retrofit/Ktor with KotlinX Serialization (already in stack) — avoid reflection-based parsers on untrusted input.

### Input validation & injection
- Room: use `@Query` with parameterized arguments. NEVER build SQL via string concatenation or `SimpleSQLiteQuery` with interpolated input.
- Deep links / `Intent` extras / `Uri` params: validate scheme, host, path, and every parameter. Reject unknown. Sanitize before use in file paths, WebViews, or queries.
- File operations from external sources: canonicalize paths (`File.canonicalPath`) and verify they are inside an allowed directory to prevent path traversal (Zip-Slip, arbitrary write).
- Never pass untrusted input to `Runtime.exec`, `ProcessBuilder`, reflection, or dynamic class loading.

### Android components & IPC
- `AndroidManifest.xml`: `android:allowBackup="false"`, `android:debuggable` never set true manually, `android:exported` explicit on every activity/service/receiver/provider. Default to `exported="false"` unless there is a documented reason otherwise.
- Exported components must validate calling package/permission and sanitize all incoming `Intent` data.
- `ContentProvider`: enforce permissions, never return data for arbitrary URIs, use selection args (no string concat).
- `PendingIntent`: always use `FLAG_IMMUTABLE` (required on API 31+) unless mutability is essential and justified.
- Request the minimum set of runtime permissions. Do not add permissions speculatively.

### WebView
- Avoid WebView entirely if the task does not require it. If required:
  - `setJavaScriptEnabled(true)` only when necessary.
  - `setAllowFileAccess(false)`, `setAllowContentAccess(false)`, `setAllowFileAccessFromFileURLs(false)`, `setAllowUniversalAccessFromFileURLs(false)`.
  - `addJavascriptInterface` only with `@JavascriptInterface`-annotated methods, only for trusted origins, never exposing sensitive APIs.
  - Load only HTTPS URLs from an allowlist. Never load arbitrary URLs from intents.

### Cryptography
- Use `androidx.security.crypto` or `javax.crypto` with vetted parameters: AES-256-GCM (never ECB, never CBC without HMAC), RSA-OAEP, HMAC-SHA-256+. MD5 and SHA-1 are forbidden for security purposes.
- Keys → Android Keystore. Never hardcoded, never derived from constants, never checked in.
- Randomness for security → `SecureRandom`. Never `Random`, `Math.random()`, or `ThreadLocalRandom`.
- IV/nonce: unique per encryption, never reused with the same key, never hardcoded.
- Don't roll your own crypto. If unsure, ask before implementing.

### Authentication & sessions
- Use Android's `BiometricPrompt` via the AndroidX library, with `CryptoObject` when unlocking Keystore-backed keys.
- Session tokens: short-lived access + rotating refresh, stored in Keystore-wrapped storage. Clear on logout.
- Do not implement custom SSO/OAuth flows from scratch — use AppAuth or the provider SDK.

### Memory leaks (Android-specific, mandatory review)
Every change involving `Context`, coroutines, Compose, listeners, or long-lived objects must pass this check:
- **ViewModel** never holds `Activity`, `View`, `Context` (except `applicationContext` via Hilt/Koin), `NavController`, or any composable lambda. Breaking this rule leaks the Activity across rotation.
- **Coroutines**: use `viewModelScope` (ViewModel) or `lifecycleScope`/`repeatOnLifecycle` (UI). NEVER `GlobalScope`. NEVER `runBlocking` on main. Cancel custom `Job`s in `onCleared`/`onDispose`.
- **Flow collection** in Compose: `collectAsStateWithLifecycle()`, not `collectAsState()`, to stop collection when the screen is not visible.
- **Compose side effects**: `LaunchedEffect` keys must reflect real dependencies. `DisposableEffect` MUST clean up every listener/callback it registers in `onDispose`. `rememberUpdatedState` for captured values in long-lived effects.
- **Listeners, observers, callbacks, broadcast receivers, sensor/location listeners, `BroadcastReceiver`, `ContentObserver`**: always paired registration/unregistration tied to lifecycle. Never register in `onCreate` and unregister in `onDestroy` for things that should be `onStart`/`onStop`.
- **Static fields**: never hold `Context`, `View`, `Activity`, `Fragment`, or anything that transitively references them. Singletons only take `applicationContext`.
- **Inner classes / anonymous classes** inside Activity/Fragment that outlive the host → use `static` equivalent (top-level class or object) with `WeakReference` to the host.
- **Bitmaps, `Cursor`, `InputStream`, `FileChannel`, `MediaPlayer`, `Camera`, `Sensor`**: always `use { }` or `try/finally close()`. Never rely on GC.
- **Compose**: do not capture `Activity`/`Context` inside `remember { }`. Use `LocalContext.current` at read time, not stored.
- Run LeakCanary in debug builds; if adding a new long-lived holder, consider whether it is leak-prone and document ownership.

### Build & ship
- Release builds: `minifyEnabled true`, `shrinkResources true`, ProGuard/R8 rules that strip `Log` and keep crash-reporting line info.
- `debuggable false`, `allowBackup false`, `usesCleartextTraffic false` in release.
- Strip unused permissions, unused exported components, unused dependencies.
- Run `./gradlew lintDebug` — treat security lint warnings as errors.

### LLM-specific anti-patterns to actively avoid
These are mistakes AI assistants (including me) repeatedly make. Treat them as forbidden:
1. Bypassing an SSL error by trusting all certificates "to make it work."
2. Using `GlobalScope.launch` because it compiles without a scope parameter.
3. Catching `Exception` and swallowing it to silence a crash.
4. Storing a token in plaintext `SharedPreferences` "for now."
5. Putting an API key in `BuildConfig` read from a checked-in gradle constant.
6. `exported="true"` left as default on new components.
7. Forgetting `FLAG_IMMUTABLE` on `PendingIntent`.
8. Using `String` to hold a password.
9. Building a SQL `WHERE` clause via `"... = '$input'"`.
10. Passing `this@Activity` into a ViewModel constructor or repository.
11. `collectAsState()` instead of `collectAsStateWithLifecycle()`.
12. Logging the full HTTP request/response in a debug interceptor without redacting auth headers.
13. `setJavaScriptEnabled(true)` + `addJavascriptInterface` + URL from an `Intent` extra.
14. Using `Random` for a token, session ID, or nonce.
15. Adding `READ_EXTERNAL_STORAGE`/`MANAGE_EXTERNAL_STORAGE` when Scoped Storage / SAF / `MediaStore` would work.
16. Parsing a deep link `Uri` and passing parameters straight into a repository or `WebView.loadUrl`.
17. Registering a listener in a composable without a matching `DisposableEffect { onDispose { ... } }`.

If you are about to do any of the above, STOP, flag it, and propose the secure alternative before writing the code.