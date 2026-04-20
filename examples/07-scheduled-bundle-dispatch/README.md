# scheduled-bundle-dispatch

Shows how OSC bundle timetags affect dispatch timing:

- start a local server
- send one bundle scheduled slightly in the future
- observe that the handler runs after the timetag becomes due

Run:

```powershell
.\gradlew.bat runExample -Pexample=scheduled-bundle-dispatch
```

This example is self-contained and prints the scheduled time, the actual receive time, and the observed delay.
