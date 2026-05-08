# tcp-client-send

Sends one OSC message over TCP using the default length-prefixed framing.

Run:

```powershell
.\gradlew.bat runExample -Pexample=tcp-client-send
```

Start a compatible TCP OSC server on `127.0.0.1:9000` before running this example.
The server must use the same framing strategy as the client.
