# message-codec

Shows how to:

- create an `OscMessage`
- encode it into an OSC byte payload
- decode the payload back into an `OscPacket`

Run:

```powershell
.\gradlew.bat runExample -Pexample=message-codec
```

Expected output includes:

- the original message
- payload size and hex bytes
- the decoded packet
