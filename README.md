<div align="center">

# KTPlus

**Kill effects for Paper**

Shop GUI, persistent unlocks, real databases, Folia support.

Players pick an effect. When they get a kill, it plays.  
Everything around that is built for a real server: economy, storage, permissions, limits, and optional hooks.

<br/>

[![Release](https://img.shields.io/github/v/release/MonkeyMoon104/KTPlus?include_prereleases&sort=semver&display_name=tag&label=release&color=0E8A16)](https://github.com/MonkeyMoon104/KTPlus/releases)
[![License](https://img.shields.io/github/license/MonkeyMoon104/KTPlus?color=blue)](LICENSE)
[![Stars](https://img.shields.io/github/stars/MonkeyMoon104/KTPlus?style=flat&color=yellow)](https://github.com/MonkeyMoon104/KTPlus/stargazers)
[![Issues](https://img.shields.io/github/issues/MonkeyMoon104/KTPlus)](https://github.com/MonkeyMoon104/KTPlus/issues)

[![Paper](https://img.shields.io/badge/Paper-1.21%2B%20%7C%2026.x-brightgreen)](https://papermc.io/)
[![Folia](https://img.shields.io/badge/Folia-supported-success)](https://papermc.io/software/folia)
[![Java](https://img.shields.io/badge/Java-21%2B-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spigot](https://img.shields.io/badge/Spigot-125998-yellow)](https://www.spigotmc.org/resources/125998/)
[![bStats Servers](https://img.shields.io/bstats/servers/26511?label=servers)](https://bstats.org/plugin/bukkit/26511)
[![bStats Players](https://img.shields.io/bstats/players/26511?label=players)](https://bstats.org/plugin/bukkit/26511)

<br/>

[Download](https://github.com/MonkeyMoon104/KTPlus/releases) ·
[SpigotMC](https://www.spigotmc.org/resources/125998/) ·
[Issues](https://github.com/MonkeyMoon104/KTPlus/issues) ·
[bStats](https://bstats.org/plugin/bukkit/26511)

</div>

---

| | |
|:--|:--|
| **Author** | [MonkeyMoon104](https://github.com/MonkeyMoon104) |
| **Commands** | `/ktplus` · `/kt` · `/killeffect` |
| **License** | [GPL-3.0](LICENSE) |

---

## Contents

- [What you get](#what-you-get)
- [Requirements](#requirements)
- [Supported versions](#supported-versions)
- [Install](#install)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [Economy](#economy)
- [Database](#database)
- [Review rewards](#review-rewards)
- [Integrations](#integrations)
- [PlaceholderAPI](#placeholderapi)
- [Developer API](#developer-api)
- [Troubleshooting](#troubleshooting)
- [License](#license)

---

## What you get

| Area | Highlights |
|------|------------|
| **Effects** | Built-in catalog (`common` → `ultra`), pricing, damage rules, perks, `/kt test` preview |
| **Players** | Shop GUI with categories, filters, preview, favorites & search |
| **Economy** | KillCoins (default) or Vault, kill rewards, persistent unlocks, optional LuckPerms grant on buy |
| **Storage** | SQLite / MySQL / MariaDB / PostgreSQL, in-game SQLite ↔ MySQL migrate |
| **Platform** | Folia-aware scheduling, WorldGuard respect, update checker, bStats, GitHub / Spigot review rewards |

---

## Requirements

| | Minimum |
|---|---|
| Server | Paper **1.21.1+** or Paper **26.x** |
| Java | **21+** |
| Folia | Supported on compatible builds |

**Optional plugins**

| Plugin | Why |
|--------|-----|
| [Vault](https://www.spigotmc.org/resources/vault.243/) | External economy |
| [LuckPerms](https://luckperms.net/) | Permission grant on purchase |
| [WorldGuard](https://enginehub.org/worldguard) | Region checks for block-changing effects |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Placeholders |

---

## Supported versions

KTPlus loads the matching NMS bridge at runtime.

| Paper |
|-------|
| 1.21 – 1.21.11 |
| 26.1.x – 26.3.x |

---

## Install

1. Put `KTPlus-*.jar` in `plugins/` — from [Releases](https://github.com/MonkeyMoon104/KTPlus/releases).
2. Start once. Configs appear under `plugins/KTPlus/`.
3. Edit what you need → `/kt reload` or restart.
4. Upgrading from classic **KT**? Keep `plugins/KT/` (the SQLite files), run **only** KTPlus, then use [`/kt import-kt`](#commands).

### Quick start

```text
/kt killcoins add <player> 1000
/kt
# buy or select an effect → get a kill
```

---

## Commands

**Base:** `/ktplus`  **Aliases:** `/kt`, `/killeffect`

| Command | Description | Permission |
|---------|-------------|------------|
| `/kt` | Open GUI | (players) |
| `/kt reload` | Reload configs | `ktplus.reload` |
| `/kt set <effect> [player]` | Select effect | `ktplus.set` / `.others` |
| `/kt clear [player]` | Clear selection | `ktplus.clear` / `.others` |
| `/kt test <effect>` | Preview at your location | `ktplus.test` |
| `/kt disable <effect>` | Hide effect from shop / use | `ktplus.disable` |
| `/kt enable <effect>` | Re-enable a disabled effect | `ktplus.enable` |
| `/kt killcoins bal [player]` | Show balance | `ktplus.killcoins` |
| `/kt killcoins add\|take\|set\|reset <player> <amount>` | Edit balance | `ktplus.killcoins.admin` |
| `/kt review <github\|spigotmc> <account>` | Claim review reward | (players) |
| `/kt migrate <sqlite\|mysql> [flags] <token>` | Migrate database | `ktplus.migrate` |
| `/kt import-kt [--dry-run] [--overwrite-balances] [token]` | Import classic KT data | `ktplus.migrate` |

<details>
<summary><strong>Migration flags</strong> (SQLite ↔ MySQL only)</summary>

```text
/kt migrate <sqlite|mysql> [--dry-run] [--include-temp-blocks]
           [--force-pending-inventory] [--allow-nonempty-target]
           <confirmation-token>
```

</details>

<details>
<summary><strong>Import from classic KT</strong> (`plugins/KT/` → KTPlus)</summary>

```text
/kt import-kt [--dry-run] [--overwrite-balances] [confirmation-token]
```

**What you need**
- **KTPlus running** (this plugin).
- The old data folder `plugins/KT/` with `kt.db` and/or `killcoins.db`.
- You do **not** need the classic KT jar loaded. Keep the folder; remove/disable the old plugin jar.

**What is imported**
- KillCoins balances → `kt_killcoins`
- Purchased effects → `kt_purchases`
- Selected effect → `kt_player_effects`

**Defaults (safe)**
- Existing balances are **not** overwritten (use `--overwrite-balances` only if you want classic balances to replace KTPlus ones).
- Existing purchases / selections are skipped.
- Unknown effect ids (renamed/removed in KTPlus) are skipped and listed in chat.
- If `economy.provider` is `VAULT`, KillCoins rows are still written to the DB but **not used in-game**.

**Steps**
1. Backup `plugins/KT/` and your KTPlus database.
2. Install/run only KTPlus; leave `plugins/KT/` on disk.
3. Dry-run: `/kt import-kt --dry-run` — check counts, unknown effects, and the confirmation token (normalized path of `plugins/KT`).
4. Apply: `/kt import-kt <token>` (add `--overwrite-balances` only if needed).
5. Verify balances / GUI, then archive or remove `plugins/KT/` when you are done.

**If something goes wrong**
- Restore your backups and retry the dry-run.
- Confirm `plugins/KT/kt.db` (and `killcoins.db` if you had it) exist and are readable.
- Classic KT **MySQL** is not supported by this command — export to SQLite first, or copy rows manually into KTPlus tables.
- Effect ids that no longer exist in KTPlus cannot be remapped automatically; unlock/select them again in KTPlus if needed.

</details>

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `ktplus.use` | true | Open `/kt` GUI and player commands |
| `ktplus.admin` | op | Admin access |
| `ktplus.admin.bypass` | op | Bypass ownership / effect locks |
| `ktplus.reload` | op | Reload |
| `ktplus.migrate` | op | DB migration / classic KT import |
| `ktplus.set` | true | Select own effect |
| `ktplus.set.others` | op | Select for others |
| `ktplus.clear` | true | Clear own effect |
| `ktplus.clear.others` | op | Clear for others |
| `ktplus.test` | op | Preview effects |
| `ktplus.grant` | op | Grant effect unlock |
| `ktplus.revoke` | op | Revoke effect unlock |
| `ktplus.killcoins` | true | View balance |
| `ktplus.killcoins.admin` | op | Edit balances |
| `ktplus.worldguard.bypass` | op | Skip WorldGuard checks |
| `ktplus.<effectId>.use` | true | Per-effect use (deny in LP to rank-lock) |

---

## Configuration

All files live under `plugins/KTPlus/`:

| File | Purpose |
|------|---------|
| `config.yml` | Global options, worlds, `cosmetic-mode`, `effects-on-mobs`, `default-language`, WorldGuard / LuckPerms, schematics |
| `lang/` | Language packs (`EN.yml`, `IT.yml`, …) — messages, GUI chrome, effect names & descriptions |
| `messages.yml` | Optional message overrides (any set key overrides lang for all players) |
| `effects.yml` | Catalog — prices, categories, perks, damage |
| `gui.yml` | GUI layout and item materials |
| `disabled-effects.yml` | Effects disabled via `/kt disable` |
| `economy.yml` | Provider, starting balance, kill rewards |
| `database.yml` | Engine, credentials, pool |
| `events.yml` | Optional random events |
| `resource-pack.yml` | Pack URL / hash / required + sound keys |
| `performance.yml` | Session limits, cooldowns, TPS throttling |

Changes apply with `/kt reload` (or a full restart).

### Disable / enable effects

Hide effects from the shop, tab-complete, `/kt set` / `/kt test`, and kill triggers without revoking purchases:

```text
/kt disable cloud
/kt enable cloud
```

Players who had that effect selected are cleared and notified; ownership stays so they can re-select after `/kt enable`.

### Language packs

On first start KTPlus creates `plugins/KTPlus/lang/` and copies `EN.yml`.

- File names must be **exactly** two uppercase letters + `.yml` (`EN.yml`, `IT.yml`, `ES.yml`).
- Players get the pack matching their client language; unknown locales use `default-language` in `config.yml` (default `EN`).
- Missing keys fall back to `EN`. Prefer editing `lang/` for translations; use `messages.yml` only for server-wide overrides.

### Cosmetic mode (visuals only)

For vanilla / SMP servers that want kill effects without gameplay impact:

```yaml
# config.yml
cosmetic-mode: true
```

When enabled, effects keep particles, sounds, and display entities, but skip damage, world blocks, potions, knockback, heals, fire ticks, and combat spawns. Random kill events in `events.yml` are separate — disable those too if you want a fully cosmetic setup.

### Player kills only

To stop kill effects on mobs (PvP-only spectacle), set:

```yaml
# config.yml
effects-on-mobs: false
```

Default is `true`. Mob KillCoins rewards in `economy.yml` are unchanged.

---

## Economy

```yaml
# economy.yml
enabled: true
provider: KILLCOINS   # or VAULT
starting-balance: 0

kill-reward:
  enabled: true
  player: 10
  mob: 1

currency:
  singular: "coin"
  plural: "coins"
```

| Provider | Notes |
|----------|-------|
| `KILLCOINS` | Internal balance in the KTPlus database |
| `VAULT` | Needs Vault + an economy plugin |

Admins: `/kt killcoins …`

---

## Database

```yaml
# database.yml
database:
  type: "sqlite"          # sqlite | mysql | mariadb | postgresql
  file: "ktplus.db"       # SQLite only
  host: "localhost"
  port: 3306
  database: "ktplus"
  username: "root"
  password: ""
  pool-size: 5            # forced to 1 on SQLite
```

| Engine | Notes |
|--------|-------|
| SQLite | Default |
| MySQL 8.0+ / MariaDB 10.5+ | `/kt migrate` supports SQLite ↔ MySQL/MariaDB |
| PostgreSQL | Supported at runtime; data move is external |

---

## Review rewards

```text
/kt review github <github-username>
/kt review spigotmc <spigot-username>
```

| Platform | Requirement | Reward |
|----------|-------------|--------|
| GitHub | Star [MonkeyMoon104/KTPlus](https://github.com/MonkeyMoon104/KTPlus) | **500** coins |
| SpigotMC | Review **4★+** on [125998](https://www.spigotmc.org/resources/125998/) | **100 × stars** (max 500) |

One claim per player / platform account.

---

## Integrations

| Hook | Notes |
|------|-------|
| WorldGuard | Respect / bypass in `config.yml` · staff: `ktplus.worldguard.bypass` |
| LuckPerms | Optional grant on purchase (`luckperms` in `config.yml`) |
| Vault | `economy.provider: VAULT` |
| PlaceholderAPI | See [placeholders](#placeholderapi) |
| Resource pack | `resource-pack.yml` — push on join, SHA-1, required or optional |

---

## PlaceholderAPI

Identifier: **`ktplus`**

<details open>
<summary><strong>Global</strong></summary>

| Placeholder | Description |
|-------------|-------------|
| `%ktplus_version%` | Plugin version |
| `%ktplus_name%` / `%ktplus_plugin%` | Plugin name |
| `%ktplus_author%` | Authors |
| `%ktplus_provider%` | Economy provider |
| `%ktplus_economy%` | Economy enabled (`yes`/`no`) |
| `%ktplus_effect_count%` | Registered effects |

</details>

<details open>
<summary><strong>Player</strong></summary>

| Placeholder | Description |
|-------------|-------------|
| `%ktplus_balance%` / `%ktplus_coins%` | Balance |
| `%ktplus_balance_formatted%` | Formatted balance |
| `%ktplus_selected%` / `%ktplus_effect%` | Selected effect id |
| `%ktplus_selected_name%` | Display name |
| `%ktplus_selected_price%` | Price |
| `%ktplus_selected_category%` | Category |
| `%ktplus_has_selected%` | Has a selection |
| `%ktplus_unlocked_count%` | Owned count |
| `%ktplus_locked_count%` | Locked count |
| `%ktplus_owns_<effectId>%` | Owns (`yes`/`no`) |
| `%ktplus_unlocked_<effectId>%` | Can activate (`yes`/`no`) |

</details>

---

## Developer API

Soft-depend on KTPlus and compile against the published `ktplus-api` artifact (same version as the plugin).

```kotlin
repositories {
    maven("https://repo.monkeymoon104.it/release")
}

dependencies {
    compileOnly("com.monkey.ktplus:ktplus-api:4.0.5")
}
```

```java
if (!KtPlusProvider.isAvailable()) {
    return;
}
KtPlus api = KtPlusProvider.get();
api.registration().register(
    Effect.builder("my-effect")
        .displayName("My Effect")
        .icon(Material.NETHER_STAR)
        .category(EffectCategory.EPIC)
        .price(500)
        .build(),
    context -> { /* play visuals */ }
);
```

Public helpers include `api.visuals()` (particles, sounds, shapes) and session spawn/track/temp-block APIs for Folia-safe multi-phase effects.

---

## Troubleshooting

| Symptom | Likely cause | What to try |
|---------|--------------|-------------|
| Effects don’t play | Perm / locked / disabled world | `ktplus.<id>.use`, unlock state, `config.yml` worlds |
| Vault balance stuck | Still on KillCoins | `economy.provider: VAULT` + Vault + economy plugin |
| Review already claimed | Player or account already used | Expected — one claim each |
| Migration rejected | Wrong target or MySQL &lt; 8 | sqlite/mysql only; upgrade MySQL |
| Classic KT data missing after upgrade | Old `plugins/KT/` not imported | `/kt import-kt --dry-run`, then with token |
| Effects still grief / damage on SMP | Cosmetic mode off / random events on | `cosmetic-mode: true` in `config.yml`; disable `events.yml` |

---

## License

Licensed under the [GNU General Public License v3.0](LICENSE).

Copyright © 2026 MonkeyMoon104

---

<div align="center">

**[GitHub](https://github.com/MonkeyMoon104/KTPlus)** ·
**[SpigotMC](https://www.spigotmc.org/resources/125998/)** ·
**[Issues](https://github.com/MonkeyMoon104/KTPlus/issues)** ·
**[bStats](https://bstats.org/plugin/bukkit/26511)**

<br/>

<sub>Built by MonkeyMoon104</sub>

</div>
