# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project follows Semantic Versioning once stable releases start.

## [0.3.0] - 2026-05-08

### Added

- Added TCP client/server transport support through `OscTransportProtocol.Tcp`.
- Added TCP packet framing strategies: `OscTcpFramingStrategy.LENGTH_PREFIXED` and `OscTcpFramingStrategy.SLIP`.
- Added client/server DSL transport selection:
  - `protocol { udp() }`
  - `protocol { tcp() }`
  - `protocol { tcp { framingStrategy = OscTcpFramingStrategy.SLIP } }`
- Added TCP backend tests and public API round-trip coverage for default length-prefixed framing and SLIP framing.

### Changed

- `OscClient` no longer accepts an external coroutine scope. Client shutdown is managed internally through `closeAsync()`, `closeAndJoin()`, and `close()`.
- `OscServer` can now be configured with UDP or TCP transport while keeping UDP as the default.

### Removed

- Removed `scope` from `OscClient` and `OscClientOptionsBuilder`.

### Migration

```kotlin
// Before
val client = OscClient(address, scope)

// After
val client = OscClient(address)
```

## [0.2.0] - 2026-04-21
### Added

- Runnable examples under `examples/`, including codec, bundle DSL, UDP client/server, routing, and scheduling use cases.

### Changed

- Reworked `OscBundle.toString()` to produce more readable output for nested bundles.
- Added nested alternation support to OSC address pattern compilation and matching.

### Fixed

- Fixed bundle timetag decoding for modern/future OSC timetag values by preserving the raw 64-bit unsigned value during bundle decode.

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
