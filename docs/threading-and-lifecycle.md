# Threading And Lifecycle

This document is the single source of truth for lifecycle semantics and concurrency guarantees of `OscServer` and `OscClient`.

## OscServer Lifecycle APIs

- `suspend fun start()`
- `fun startAsync(): Deferred<Unit>`
- `suspend fun stop()`
- `fun stopAsync(): Deferred<Unit>`
- `override fun close()`

### Concurrency Guarantees

- `start()` / `startAsync()` are not guaranteed to be concurrency-safe.
- `stopAsync()` is concurrency-safe and idempotent.
  Concurrent callers receive/await the same stop task.
- `stop()` awaits `stopAsync()`.
- `close()` triggers `stopAsync()` (fire-and-forget).

### One-Shot Semantics

`OscServer` is one-shot:

- after stop completes, calling `start()` again throws `OscLifecycleException`.

### Recommended Usage

Startup:

```kotlin
server.start()
```

Shutdown:

```kotlin
server.stop()
```

Avoid concurrent startup calls from multiple coroutines.

## OscClient Lifecycle APIs

- `suspend fun send(packet: OscPacket)`
- `fun closeAsync(): Deferred<Unit>`
- `suspend fun closeAndJoin()`
- `override fun close()`

### Concurrency Guarantees

- `closeAsync()` is concurrency-safe and idempotent.
  Concurrent callers receive/await the same close task.
- `closeAndJoin()` awaits full shutdown.
- `close()` triggers non-blocking shutdown.

### Recommended Usage

- In structured coroutine code, prefer `closeAndJoin()`.
- For `Closeable` integration / best-effort shutdown paths, use `close()`.

## Dispatcher / Scope Notes

- Server dispatch concurrency is controlled by `maxConcurrentDispatches`.
- Dispatch jobs run on `dispatchDispatcher` when concurrent mode is enabled.
- Provide your own `CoroutineScope` only if you need explicit parent lifecycle control.
