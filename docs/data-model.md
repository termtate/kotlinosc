# Data Model And DSL

## Core Packet Types

- `OscPacket` (sealed packet abstraction)
- `OscMessage(address: String, args: List<OscArg>)`
- `OscBundle(timeTag: OscTimetag, elements: List<OscPacket>)`

## Message Construction

### Companion invoke helpers

`OscMessage` provides companion `invoke` overloads:

- `OscMessage("/path", vararg OscArg)`
- `OscMessage("/path", vararg Any?)`
- `OscMessage("/path", List<Any?>)`
- `OscMessage("/path", List<OscArg>)`

`Any?` arguments are boxed into `OscArg` internally.

## Bundle DSL

Entry:

- `oscBundle(timetag = OscTimetag.IMMEDIATELY) { ... }`

Builder:

- `message(address, vararg OscArg)`
- `message(address, List<OscArg>)`
- `message(address, vararg Any?)`
- `message(address, List<Any?>)`
- `bundle(timetag = OscTimetag.IMMEDIATELY) { ... }`

Example:

```kotlin
val packet = oscBundle {
    message("/a", 1, "x")
    bundle {
        message("/b", true)
    }
}
```

## Kotlin Type Boxing

see [type-support-matrix.md](type-support-matrix.md).
