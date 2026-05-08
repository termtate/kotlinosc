# Type Support Matrix

This document summarizes the OSC argument and packet-related type support currently implemented by `kotlinosc`.


> packet/data encoding and decoding behavior is currently aligned with OSC 1.0

## OSC Argument Type Matrix

| OSC type | Wrapper type | Type tag | Encode | Decode | Kotlin auto-boxing |
| --- | --- | --- | --- | --- | --- |
| Int32 | `OscInt32` | `i` | Yes | Yes | `Int` |
| Float32 | `OscFloat32` | `f` | Yes | Yes | `Float` |
| String | `OscString` | `s` | Yes | Yes | `String` |
| Blob | `OscBlob` | `b` | Yes | Yes | `ByteArray` |
| Int64 | `OscInt64` | `h` | Yes | Yes | `Long` |
| Float64 | `OscFloat64` | `d` | Yes | Yes | `Double` |
| Char | `OscChar` | `c` | Yes | Yes | `Char` |
| RGBA | `OscRGBA` | `r` | Yes | Yes | `RGBA` |
| MIDI | `OscMIDI` | `m` | Yes | Yes | `MIDI` |
| Timetag | `OscTimetag` | `t` | Yes | Yes | `Instant` |
| True | `OscTrue` | `T` | Yes | Yes | `Boolean(true)` |
| False | `OscFalse` | `F` | Yes | Yes | `Boolean(false)` |
| Nil | `OscNil` | `N` | Yes | Yes | `null` |
| `I` tag | `OscInfinitum` | `I` | Yes | Yes | no direct Kotlin primitive mapping |
| Symbol | `OscSymbol` | `S` | Yes | Yes | not auto-boxed from plain `String` |
| Array | `OscArray` | `[` ... `]` | Yes | Yes | `List<*>`, `Array<*>` |

## Notes On `I`

The library currently exposes OSC type tag `I` as `OscInfinitum`.


- the public naming stays `OscInfinitum`
- some OSC 1.1 discussions describe `I` as impulse / bang
- this library currently keeps the existing code naming and behavior for compatibility

If interoperability depends on the semantic interpretation of `I`, document that expectation explicitly at the application level.
