# Transport

`kotlinosc` supports UDP and TCP transports for OSC clients and servers.

UDP is the default transport. TCP can be selected explicitly through the public API or DSL.

## UDP

UDP sends each OSC packet as one datagram. It is the default because OSC is commonly used with UDP and because it has no connection lifecycle.

```kotlin
val server = oscServer("127.0.0.1", 9000)
val client = oscClient("127.0.0.1", 9000)
```

UDP is a good fit when packet loss is acceptable and low overhead matters.

## TCP

TCP sends OSC packets over a persistent byte stream. Since TCP does not preserve packet boundaries, each OSC packet must be framed.

Use TCP from the DSL:

```kotlin
val server = oscServer("127.0.0.1", 9000) {
    protocol { tcp() }
}

val client = oscClient("127.0.0.1", 9000) {
    protocol { tcp() }
}
```

Use TCP from the client constructor:

```kotlin
val protocol = OscTransportProtocol.Tcp()

val client = OscClient(
    targetAddress = address,
    protocol = protocol
)
```

The TCP client connects lazily on the first `send`. The TCP server accepts multiple clients.

> Note that `OscServer` does not have a public constructor API.

## TCP Framing

Client and server must use the same TCP framing strategy.

### Length-Prefixed

`OscTcpFramingStrategy.LENGTH_PREFIXED` is the default TCP framing strategy.

Each OSC packet is prefixed with a 32-bit big-endian payload length.

```kotlin
protocol { tcp() }
```

This is equivalent to:

```kotlin
protocol {
    tcp {
        framingStrategy = OscTcpFramingStrategy.LENGTH_PREFIXED
    }
}
```

### SLIP

`OscTcpFramingStrategy.SLIP` uses SLIP END/ESC framing and byte escaping.

```kotlin
protocol {
    tcp {
        framingStrategy = OscTcpFramingStrategy.SLIP
    }
}
```

## Error Handling

Transport hooks apply to both UDP and TCP.

- Payload decode failures call `OscTransportHook.onDecodeError`.
- TCP framing failures call `OscTransportHook.onTransportError` and close the affected client connection.
- A TCP server can continue accepting and reading other clients after one client fails framing.

## Shutdown

Use structured shutdown APIs:

```kotlin
client.closeAndJoin()
server.stop()
```

`close()` and `stopAsync()` are non-blocking triggers. Use `closeAndJoin()` or `stop()` when the caller must wait for shutdown to complete.
