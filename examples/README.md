# Examples

These examples are small, runnable use cases for `kotlinosc`.

## Run An Example

From the project root:

```bash
./gradlew runExample -Pexample=message-codec
```

On Windows:

```powershell
.\gradlew.bat runExample -Pexample=message-codec
```

Available example ids:

- `message-codec`
- `bundle-dsl`
- `udp-client-send`
- `udp-server-route`
- `client-server-roundtrip`
- `address-pattern-routing`
- `scheduled-bundle-dispatch`
- `tcp-client-send`
- `tcp-server-route`

## Pick By Goal

- Build a message and inspect the encoded payload: `message-codec`
- Build nested bundles with the DSL: `bundle-dsl`
- Send one UDP OSC message: `udp-client-send`
- Start a UDP server and handle one route: `udp-server-route`
- See a full in-process roundtrip: `client-server-roundtrip`
- See address pattern routing in action: `address-pattern-routing`
- See delayed bundle dispatch with OSC timetags: `scheduled-bundle-dispatch`
- Send one TCP OSC message with default length-prefixed framing: `tcp-client-send`
- Start a TCP server using SLIP framing and handle one route: `tcp-server-route`
