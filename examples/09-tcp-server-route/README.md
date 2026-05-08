# tcp-server-route

Starts a TCP OSC server using SLIP framing:

- start an OSC server
- select TCP transport
- configure `OscTcpFramingStrategy.SLIP`
- register one route
- wait for one matching message
- stop the server

Run:

```powershell
.\gradlew.bat runExample -Pexample=tcp-server-route
```

In another terminal or OSC tool, send a SLIP-framed TCP OSC message to `127.0.0.1:9000`
with address `/demo/tcp/slip`.
