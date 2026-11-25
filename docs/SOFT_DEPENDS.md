# Soft-depend evaluation (Folia)

KTPlus supports **Folia**. Optional libraries must never become hard dependencies unless they are Folia-safe.

## Already integrated

| Soft-depend | Status | Notes |
|-------------|--------|-------|
| **ParticlePlus** | Soft-hook in runtime | Keep optional; do not require at compile/runtime. |
| **PacketEvents** | Shaded | Required for packet-based features already in tree. |

## Evaluated — do **not** hard-depend yet

| Library | Claimed Folia | Verdict |
|---------|---------------|---------|
| **DisplayMotion** | No | Reject as primary/hard dependency. Breaks Folia servers. |
| **ParticleLib** (sun-plugins) | Claimed yes (alpha) | Interesting shapes API, but alpha / unstable. Revisit only after a Folia spike that spawns helix/torus on a Folia region thread without scheduler APIs that Folia forbids. Prefer internal `FxGeometry` / `VisualEffectService` until then. |
| **HologramLib** | Yes | Useful for text/item/block holograms; less suited as the core kill-FX engine. Optional soft-depend only after a Folia spike (entity/packet spawn + cleanup on region thread). |
| **DisplayEntityUtils** | Unverified | Soft-depend candidate only; verify Folia before any optional hook. |

## Spike checklist (before any soft-depend PR)

1. Load library on Paper **and** Folia with the same jar.
2. Spawn particles / displays from a kill-effect session using KTPlus `EffectSession` timers only (no `BukkitRunnable` / global scheduler assumptions).
3. Confirm cleanup on session cancel and region unload.
4. Keep the dependency `compileOnly` + soft detect; never shade unless already required.

Until a spike passes, keep geometry in `com.monkey.ktplus.effects.visual.display` and `VisualEffectService`.
