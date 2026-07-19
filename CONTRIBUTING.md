# Contributing

Thanks for your interest in Offhand!

## Ground rules

- **Privacy first.** No feature may send user content (audio, transcripts, notes) off the
  device. PRs that add network calls beyond the model download and opt-in crash reporting
  will not be accepted.
- **Build must be green**: `./gradlew assembleDebug testDebugUnitTest lintDebug :app:lintDevDebug`.
- Unit tests are required for use cases and ViewModel logic (Arrange-Act-Assert, MockK).

## Code style

- Kotlin, Jetpack Compose, three layers per feature: domain → data → presentation.
- ViewModels extend `BaseViewModel`, expose a single content-only `UiState` via StateFlow.
- Features depend on `:core:ai-api` abstractions, never on `:core:ai-local`.
- No code comments — self-documenting naming (constraint notes are the only exception).
- User-facing strings go in `strings.xml`.
- No wildcard imports.

## Setup

See [README.md](README.md#building) — no accounts or tokens needed; the app downloads
its models (Whisper + Qwen3, both ungated) on first run.

## Reporting issues

Include device model, RAM, Android version, and the selected acceleration backend
(Settings → AI acceleration). Never attach recordings or notes containing sensitive data.
