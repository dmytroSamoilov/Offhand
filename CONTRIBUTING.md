# Contributing

Thanks for your interest in Offhand!

## Open source, not open contribution

Offhand's source is public under [GPL-3.0](LICENSE) so anyone can verify the privacy
claims — nothing leaves the device — and build the app themselves. However, **outside
code contributions (pull requests) are not accepted.**

Keeping the codebase under a single copyright holder is what allows Offhand to be
distributed through app stores whose terms are incompatible with GPL-3.0 (such as the
Apple App Store) while the repository itself stays GPL-3.0. Accepting third-party code
under GPL-3.0 would make that impossible. SQLite follows the same model.

Pull requests will be closed with a pointer to this document — please don't take it
personally. Forking is welcome within the terms of the license (note the name/logo
restrictions in [README.md](README.md#license)).

## What is welcome

- **Bug reports and feature ideas** via GitHub issues.
- **Security reports** — please use GitHub's private vulnerability reporting (the
  repository's Security tab) rather than a public issue.

When reporting issues, include device model, RAM, Android version, and the selected
acceleration backend (Settings → AI acceleration). Never attach recordings or notes
containing sensitive data.

## Building it yourself

See [README.md](README.md#building) — no accounts or tokens needed; the app downloads
its models (Whisper + Gemma, both ungated) on first run.
