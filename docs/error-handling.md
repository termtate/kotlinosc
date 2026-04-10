# Error Handling

This document summarizes error types, where they can happen, and recommended handling in user code.

## Exception Hierarchy

Base:

- `OscException`

Codec and parsing:

- `OscCodecException`
- `OscPacketIllegalBytesException`
- `OscTypeTagParseException`
- `OscBundleParseException`
- `OscArrayTagParseException`
- `OscUnknownTypeTagException`
- `OscBufferUnderflowException`
- `OscAddressParseException`

Runtime/lifecycle:

- `OscLifecycleException`
- `OscTransportException`
- `OscDispatchException`

## Where Errors Occur

### Encode/Decode

Potential sources:

- `OscPacket.encodeToByteArray(...)`
- `OscPacket.decodeFromByteArray(...)`
- transport receive decode pipeline

Typical failures:

- invalid payload bytes
- invalid or unknown type tags
- malformed bundle or address

### Transport

Potential sources:

- UDP send/receive loop
- socket close during active IO

Typical failures:

- socket/network IO errors
- close/race conditions during shutdown

### Routing and Dispatch

Potential sources:

- route handler user code
- scheduler dispatch loop

Behavior is controlled by:

- `continueOnDispatchError`

If `continueOnDispatchError = true`:

- dispatch errors are logged and runtime continues.

If `continueOnDispatchError = false`:

- dispatch can fail fast with `OscDispatchException`.

## Hooks

Use `OscTransportHook` for transport/decode observability:

- `onDecodeError(payload, error)`
- `onTransportError(error)`

Recommendations:

- keep hook code fast and non-blocking
- avoid throwing from hooks
- include context (remote endpoint, packet summary) in your logs/metrics

## Lifecycle Error Rules

- `OscServer.start()` after shutdown throws `OscLifecycleException`.
- Non-blocking stop/close APIs are idempotent and share one async task.

## Recommended Production Strategy

1. Keep `continueOnDispatchError = true` unless fail-fast is explicitly required.
2. Implement `OscTransportHook` and route logs to your monitoring system.
3. Wrap top-level startup/shutdown in `try/catch` and always perform graceful stop.
4. Add tests for malformed packet input and handler exceptions.
