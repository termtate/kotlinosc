# kotlinosc

`kotlinosc` is a Kotlin/JVM OSC (Open Sound Control) library focused on:

- OSC packet codec (`OscMessage`, `OscBundle`)
- OSC address pattern matching and routing
- UDP server/client runtime
- Kotlin-friendly DSL for server/client and bundle construction

## Features

- Lightweight: runtime dependency is only `kotlin-logging`.
- Broad unit-test coverage.


## Current Status

- `0.1.0` release content is prepared and ready for first release tag.
- Public API is guarded by binary-compatibility checks (`apiCheck` / `apiDump`).
- Maven Central + GitHub Release automation is configured via GitHub Actions (tag-triggered release).

## Installation

> Artifact coordinates are finalized and will become available on Maven Central after the first release is published.

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.termtate.kotlinosc:kotlinosc:0.1.0")
}
```

### Gradle (Groovy DSL)

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.github.termtate.kotlinosc:kotlinosc:0.1.0'
}
```

### Maven

```xml
<dependency>
  <groupId>io.github.termtate.kotlinosc</groupId>
  <artifactId>kotlinosc</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Build And Test

```bash
./gradlew test
./gradlew apiCheck
```

On Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat apiCheck
```

## Quick Start

### Build an OSC message

```kotlin
import io.github.termtate.kotlinosc.type.OscMessage

val msg = OscMessage("/synth/freq", 440, 0.8f, "lead")
```

### Build an OSC bundle (DSL)

```kotlin
import io.github.termtate.kotlinosc.arg.OscTimetag
import io.github.termtate.kotlinosc.type.oscBundle

val bundle = oscBundle(OscTimetag.IMMEDIATELY) {
    message("/a", 1, 2.0f, "x")
    bundle {
        message("/b", true)
    }
}
```

### Start a server with DSL

```kotlin
import io.github.termtate.kotlinosc.transport.dsl.oscServer

val server = oscServer("127.0.0.1", 9000) {
    route {
        on("/ping") { message ->
            println("received: ${message.address}")
        }
    }
}

server.startAsync()
// ...
server.stop()
```

### Send with client DSL

```kotlin
import io.github.termtate.kotlinosc.transport.dsl.oscClient
import io.github.termtate.kotlinosc.type.OscMessage

val client = oscClient("127.0.0.1", 9000)
client.send(OscMessage("/ping"))
client.closeAndJoin()
```

## Documentation

- Threading and lifecycle: [docs/threading-and-lifecycle.md](docs/threading-and-lifecycle.md)
- Data model and DSL: [docs/data-model.md](docs/data-model.md)
- Configuration: [docs/config.md](docs/config.md)
- Address pattern syntax: [docs/address-pattern.md](docs/address-pattern.md)
- Error handling: [docs/error-handling.md](docs/error-handling.md)
- Changelog: [CHANGELOG.md](CHANGELOG.md)

## Current Limitations And Planned Features

Not supported yet (planned for future releases):

- Address pattern nested alternation.
- TCP server/client transport support.

## Publishing Setup

Publishing infrastructure is configured for Maven Central (Portal) with signing.

Before publishing, provide credentials in `~/.gradle/gradle.properties` or CI secrets:

```properties
mavenCentralUsername=...
mavenCentralPassword=...
signingInMemoryKey=...
signingInMemoryKeyPassword=...
```

Validation commands:

```bash
./gradlew apiCheck
./gradlew publishToMavenLocal
```

Release publishing command (enables signing):

```bash
./gradlew publishToMavenCentral -Prelease
```

GitHub Actions release flow:

- Push a tag like `v0.1.0`
- Workflow publishes artifacts to Maven Central
- Workflow creates GitHub Release notes from `CHANGELOG.md`

## License

MIT. See [LICENSE](LICENSE).
