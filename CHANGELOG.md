# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project follows Semantic Versioning once stable releases start.

## [Unreleased]

### Added

- No unreleased changes.

## [0.1.0] - 2026-04-11

### Added

- Initial Kotlin/JVM OSC library release.
- OSC data model: `OscPacket`, `OscMessage`, `OscBundle`, and OSC argument types.
- Codec APIs: `encodeToByteArray` and `decodeFromByteArray`.
- Routing and pattern matching: `OscRouter` and OSC address matcher/compiler.
- UDP runtime: `OscServer`, `OscClient`, and Kotlin DSL builders.
- Lifecycle APIs with async idempotent shutdown (`stopAsync` / `closeAsync`).
- Project documentation under `docs/` and public API compatibility checks (`apiCheck` / `apiDump`).

### Changed

- `OscTimetag.toInstant()` now rejects `OscTimetag.IMMEDIATELY` sentinel values.

### Fixed

- Prevented incorrect timestamp conversion when `OscTimetag.IMMEDIATELY` was converted as an absolute timestamp.
