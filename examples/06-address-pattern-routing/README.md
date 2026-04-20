# address-pattern-routing

Shows how OSC address patterns affect routing:

- exact and wildcard route matching
- alternation with `{a,b}`
- character classes with `[0-9]`

Run:

```powershell
.\gradlew.bat runExample -Pexample=address-pattern-routing
```

This example is self-contained and prints which handlers matched each message.
