# KTPlus vendor Maven repo

Patched third-party artifacts consumed by this build until upstream releases the fixes.

## Inventory Framework `0.12.1-ktplus.1`

- Fixes `ArrayIndexOutOfBoundsException` in `Gui.processMethodAnnotations` when
  zero-parameter methods (e.g. `update()`, `copy()`) are scanned on `ChestGui`.
- Only access `getParameterTypes()[0]` when `parameterCount > 0`.
- Upstream fix merged in [PR #2549](https://github.com/stefvanschie/IF/pull/2549)
  (2026-09-10) but **not released** yet (latest Maven release is still `0.12.1`).

Remove this artifact and revert `lib-inventory-framework` in `gradle/libs.versions.toml`
to the official version once IF publishes `0.12.2` (or later) with the fix.

## paperweight-userdev

Previously vendored as `2.0.0-beta.23-ktplus.1`. Replaced by upstream
`2.0.0-beta.24` (Gradle 10 `registering` deprecation fixed).
