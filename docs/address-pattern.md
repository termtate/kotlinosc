# Address Pattern

This document describes the OSC address pattern syntax supported by `kotlinosc` routing and matcher components.

## Scope

Pattern support is used by route registration and matching:

- `OscRouter.on(pattern, ...)`
- internal address compiler/matcher (`OscAddressCompiler` / `OscAddressMatcher`)

## Basic Rules

- A pattern must start with `/`.
- Pattern is split into segments by `/`.
- Empty segments are invalid (for example `//a`).

## Segment Syntax

Within one segment, these tokens are supported.

### Literal

Any normal character is treated as a literal and must match exactly.

Example:

- Pattern segment: `foo`
- Matches only: `foo`

### `*` (Star)

Matches zero or more characters in a segment.

Examples:

- `f*` matches `f`, `foo`, `f123`
- `*bar` matches `bar`, `foobar`

### `?` (Any Single Char)

Matches exactly one character in a segment.

Examples:

- `a?c` matches `abc`, `a0c`
- `a?c` does not match `ac`

### Character Class `[ ... ]`

Matches exactly one character from a set or range.

Examples:

- `[abc]` matches one of `a`, `b`, `c`
- `[a-z]` matches one lowercase letter
- `[!0-9]` or `[^0-9]` matches one non-digit

Notes:

- Empty class `[]` is invalid.
- Invalid descending range like `[z-a]` is invalid.

### Alternation `{a,b,c}`

Matches one branch from comma-separated alternatives.

Examples:

- `{foo,bar}` matches `foo` or `bar`
- `pre{a,b}post` matches `preapost` or `prebpost`

Current limitation:

- Nested alternation is not supported.

## Invalid Patterns (examples)

- `abc` (does not start with `/`)
- `/foo//bar` (empty segment)
- `/foo[bar` (unclosed char class)
- `/foo{a,b` (unclosed alternation)

## Strict vs Permissive Matching

Address pattern strictness is configured by:

- `OscConfig.AddressPattern.strictAddressPattern`

Behavior:

- `true`: malformed address/pattern input causes matching error.
- `false`: matcher tries to be permissive for malformed input.

## Tips

- Prefer explicit route patterns in production.
- Keep wildcard-heavy patterns narrow to avoid accidental broad matches.
- If you expose route patterns to users, validate and test them before runtime.
