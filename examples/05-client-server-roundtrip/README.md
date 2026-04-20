# client-server-roundtrip

Shows a complete in-process flow:

- start a server on a temporary port
- send one message from a client
- verify the server receives it

Run:

```powershell
.\gradlew.bat runExample -Pexample=client-server-roundtrip
```

This example is self-contained and does not require any external OSC tool.
