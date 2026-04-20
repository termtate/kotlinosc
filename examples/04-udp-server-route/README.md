# udp-server-route

Shows the smallest possible UDP server flow:

- start an OSC server
- register one route
- wait for one matching message
- stop the server

Run:

```powershell
.\gradlew.bat runExample -Pexample=udp-server-route
```

In another terminal or OSC tool, send a message to `127.0.0.1:9000` with address `/demo/ping`.
