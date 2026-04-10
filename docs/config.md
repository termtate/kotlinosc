# Configuration

Core config model: `io.github.termtate.kotlinosc.config.OscConfig`

## Codec

Type: `OscConfig.Codec`

- `strictCodecPayloadConsumption: Boolean`

Default:

```kotlin
OscConfig.Codec.default // strictCodecPayloadConsumption = true
```

Meaning:

- `true`: decoder enforces strict payload consumption checks.
- `false`: decoder is more permissive with payload tail/consumption behavior.

## Address Pattern

Type: `OscConfig.AddressPattern`

- `strictAddressPattern: Boolean`

Default:

```kotlin
OscConfig.AddressPattern.default // strictAddressPattern = true
```

Meaning:

- `true`: invalid incoming address patterns are treated as errors during matching.
- `false`: matcher is more permissive for malformed patterns.

## Using Config In DSL

### Server DSL

```kotlin
val server = oscServer("127.0.0.1", 9000) {
    codec {
        strictCodecPayloadConsumption = true
    }
    addressPattern {
        strictAddressPattern = true
    }
}
```

### Client DSL

```kotlin
val client = oscClient("127.0.0.1", 9000) {
    codec {
        strictCodecPayloadConsumption = true
    }
}
```
