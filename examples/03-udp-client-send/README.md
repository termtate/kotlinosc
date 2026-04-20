# udp-client-send

Shows the smallest possible UDP client flow:

- create an OSC client
- send one message
- close the client

Run:

```powershell
.\gradlew.bat runExample -Pexample=udp-client-send
```

This example sends a message to `127.0.0.1:9000`. Start any OSC receiver on that port first if you want to observe the packet externally.
