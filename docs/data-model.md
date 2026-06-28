# Data Model And DSL

## Core Packet Types

- `OscPacket` (sealed packet abstraction)
- `OscMessage(address: String, args: List<OscArg>)`
- `OscBundle(timeTag: OscTimetag, elements: List<OscPacket>)`

## Message Construction

### User-value helper

Use `oscMessageOf` when building messages from regular Kotlin values:

```kotlin
val message = oscMessageOf("/path", 1, "x", true)
```

Each argument is boxed into `OscArg` internally. `OscArg` values are used unchanged.

`List<*>` and `Array<*>` values are boxed as a single `OscArray` argument. To pass
an existing collection as multiple message arguments, spread it explicitly:

```kotlin
val args = listOf(1, "x")
val message = oscMessageOf("/path", *args.toTypedArray())
```

### Pre-boxed arguments

Use the `OscMessage` constructor directly when you already have boxed OSC arguments:

```kotlin
val message = OscMessage("/path", listOf(OscInt32(1), OscString("x")))
```

## Bundle DSL

Entry:

- `buildOscBundle(timetag = OscTimetag.IMMEDIATELY) { ... }`

Builder:

- `message(address, vararg Any?)`
- `bundle(timetag = OscTimetag.IMMEDIATELY) { ... }`
- `packet(packet: OscPacket)`

Example:

```kotlin
val packet = buildOscBundle {
    message("/a", 1, "x")
    bundle {
        message("/b", true)
    }
    packet(oscMessageOf("/c"))
}
```

`message` uses the same boxing rules as `oscMessageOf`; list and array arguments
become a single `OscArray` argument. Use `packet` to add a prebuilt `OscMessage`
or `OscBundle` directly.

## Kotlin Type Boxing

see [type-support-matrix.md](type-support-matrix.md).
