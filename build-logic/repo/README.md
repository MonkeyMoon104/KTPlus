# KTPlus vendor Maven repo

Patched third-party artifacts consumed by this build until upstream releases the fixes.

## paperweight-userdev `2.0.0-beta.23-ktplus.1`

- Replaces deprecated Kotlin DSL `by tasks.registering` with `tasks.register` in
  `PaperweightUser` (`cleanCache`, `reobfJar`).
- Upstream: https://github.com/PaperMC/paperweight/pull/405

## Inventory Framework `0.12.1-ktplus.1`

- Fixes `ArrayIndexOutOfBoundsException` in `Gui.processMethodAnnotations` when
  zero-parameter methods (e.g. `update()`, `copy()`) are scanned on `ChestGui`.
- Only access `getParameterTypes()[0]` when `parameterCount > 0`.

Remove the matching artifacts and revert versions in `gradle/libs.versions.toml`
once upstream publishes the same fixes.
