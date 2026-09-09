<div align="center">

# KTPlus

**Kill effects for Paper** — shop GUI, persistent unlocks, real databases, Folia support.

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
| **Default branch** | [`enhanced`](https://github.com/MonkeyMoon104/KTPlus/tree/enhanced) |
| **License** | [GPL-3.0](LICENSE) |

---

## Contents

<details open>
<summary><strong>For server owners</strong></summary>

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
- [Troubleshooting](#troubleshooting)

</details>

<details>
<summary><strong>For developers</strong></summary>

- [Build from source](#build-from-source)
- [Project layout](#project-layout)
- [Development](#development)
- [License](#license)

</details>

---

## What you get

| Area | Highlights |
|------|------------|
| **Effects** | 71 built-ins (`common` → `ultra`), pricing, damage rules, perks, sessions with blocks / particles / entities / schematics, `/kt test` preview |
| **Players** | Shop GUI with categories & filters, IF backend + vanilla fallback, optional resource pack for custom sounds |
| **Economy** | KillCoins (default) or Vault, kill rewards, persistent unlocks, optional LuckPerms grant on buy |
| **Storage** | SQLite / MySQL / MariaDB / PostgreSQL, Flyway on startup, in-game SQLite ↔ MySQL migrate |
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

**To build:** JDK 21 (main/core), JDK 25 toolchain for Paper 26.x NMS, Gradle 9.7.1 (wrapper).

---

## Supported versions

KTPlus loads the matching NMS bridge at runtime. `plugin.yml` uses `api-version: '1.21'`.

| Server | Module | Server | Module |
|--------|--------|--------|--------|
| 1.21 / 1.21.1 | `v1_21_1` | 1.21.8 | `v1_21_8` |
| 1.21.3 | `v1_21_3` | 1.21.9 | `v1_21_9` |
| 1.21.4 | `v1_21_4` | 1.21.10 | `v1_21_10` |
| 1.21.5 | `v1_21_5` | 1.21.11 | `v1_21_11` |
| 1.21.6 | `v1_21_6` | 26.1.x | `v26_1` |
| 1.21.7 | `v1_21_7` | 26.2.x (+ other 26.x) | `v26_2` |

---

## Install

1. Put `KTPlus-4.0.0.jar` in `plugins/` — from [Releases](https://github.com/MonkeyMoon104/KTPlus/releases) or [build it](#build-from-source).
2. Start once. Configs appear under `plugins/KTPlus/`.
3. Edit what you need → `/kt reload` or restart.
4. Upgrading from classic **KT**? Keep `plugins/KT/` (the SQLite files), run **only** KTPlus, then use [`/kt import-kt`](#commands).

> On first load, Paper downloads libraries declared in `plugin.yml`. PacketEvents is prepared by KTPlus’ own runtime loader.

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
| `ktplus.admin` | op | Admin access |
| `ktplus.admin.bypass` | op | Bypass restrictions |
| `ktplus.reload` | op | Reload |
| `ktplus.migrate` | op | DB migration / classic KT import |
| `ktplus.set` | true | Select own effect |
| `ktplus.set.others` | op | Select for others |
| `ktplus.clear` | true | Clear own effect |
| `ktplus.clear.others` | op | Clear for others |
| `ktplus.test` | op | Preview effects |
| `ktplus.killcoins` | true | View balance |
| `ktplus.killcoins.admin` | op | Edit balances |
| `ktplus.worldguard.bypass` | op | Skip WorldGuard checks |
| `ktplus.<effectId>.use` | — | Per-effect use |

---

## Configuration

All files live under `plugins/KTPlus/`:

| File | Purpose |
|------|---------|
| `config.yml` | Global options, worlds, WorldGuard / LuckPerms, schematics |
| `messages.yml` | Messages |
| `effects.yml` | Catalog — prices, categories, perks, damage |
| `economy.yml` | Provider, starting balance, kill rewards |
| `gui.yml` | GUI layout and items |
| `database.yml` | Engine, credentials, pool |
| `events.yml` | Optional random events |
| `resource-pack.yml` | Pack URL / hash / required + sound keys |
| `performance.yml` | Session limits, cooldowns, TPS throttling |

Changes apply with `/kt reload` (or a full restart).

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

| Engine | Runtime | Flyway | `/kt migrate` |
|--------|:-------:|:------:|:-------------:|
| SQLite | yes (default) | yes | ↔ MySQL |
| MySQL 8.0+ | yes | yes | ↔ SQLite |
| MariaDB | yes | yes | — |
| PostgreSQL | yes | yes | — |

Schema is applied with Flyway Java migrations on startup. MariaDB / PostgreSQL need external tools for data moves.

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

Player starts a session with their **platform** username (not the Minecraft IGN). KTPlus polls for up to **300s** (every **15s**). On success: claim stored, coins paid, short broadcast. One claim per player / platform account — already-claimed responses stay quiet (message only).

---

## Integrations

| Hook | Notes |
|------|-------|
| WorldGuard | Respect / bypass in `config.yml` · staff: `ktplus.worldguard.bypass` |
| LuckPerms | Optional grant on purchase (`luckperms` in `config.yml`) |
| Vault | `economy.provider: VAULT` |
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

## Build from source

```bash
git clone https://github.com/MonkeyMoon104/KTPlus.git
cd KTPlus
git checkout enhanced

./gradlew build        # Linux / macOS
gradlew.bat build      # Windows
```

**Output:** `dist/build/libs/KTPlus-4.0.0.jar`

| Task | |
|------|---|
| `./gradlew build` | Full build (includes `:dist`) |
| `./gradlew test` | Unit tests (`:core:test`) |
| `./gradlew :dist:shadowJar` | Shaded plugin jar |
| `./gradlew :dist:verifyPluginJar` | Relocations & library descriptors |

Main/core compiles with **Java 21**. Paper **26.x** NMS modules use a **Java 25** toolchain.

> Temporary patched **paperweight** and **Inventory Framework** builds live under `build-logic/repo` until upstream merges. Details: [`build-logic/repo/README.md`](build-logic/repo/README.md).

---

## Project layout

```text
KTPlus/
├── build-logic/     Gradle conventions + vendor Maven repo
├── common/          Shared contracts
├── nms-bridge/      Version → NMS mapping
├── NMS/             v1_21_* (reobf) · v26_* (Mojmap)
├── core/            Logic, configs, effects, GUI, storage
├── dist/            Distributable shaded jar
├── gradle/          Version catalog + wrapper
└── resourcepacks/   Pack assets
```

| Library kind | Mechanism | Examples |
|--------------|-----------|----------|
| Paper `libraries:` | Downloaded by Paper | Jackson, HikariCP, Flyway, OkHttp, JDBC, Lamp |
| Shaded | Shadow → `com.monkey.ktplus.libs.*` | IF, bStats, ASM, Querz NBT, jar-relocator |
| Custom loader | Download / relocate on `onLoad` | PacketEvents |

---

## Development

- Default branch: **`enhanced`** (force-push and deletion disabled)
- Dependabot: weekly (Mon, Europe/Rome) on the Gradle catalog; vendored / Paper pins ignored — [`.github/dependabot.yml`](.github/dependabot.yml)

| Library | Issue | Status |
|---------|-------|--------|
| paperweight-userdev | Gradle 10 `by registering` deprecation | Vendored · [PR #405](https://github.com/PaperMC/paperweight/pull/405) |
| Inventory Framework 0.12.1 | AIOOBE in `processMethodAnnotations` | Vendored · [PR #2553](https://github.com/stefvanschie/IF/pull/2553) |

---

## Troubleshooting

| Symptom | Likely cause | What to try |
|---------|--------------|-------------|
| Disables on load | PacketEvents download/relocate failed | Console `[Library]` errors; Maven mirror access |
| `Library GUI unavailable` | Inventory Framework failure | Build that includes the patched IF vendor artifact |
| Effects don’t play | Perm / locked / disabled world | `ktplus.<id>.use`, unlock state, `config.yml` worlds |
| Vault balance stuck | Still on KillCoins | `economy.provider: VAULT` + Vault + economy plugin |
| Review already claimed | Player or account already used | Expected — one claim each |
| Migration rejected | Wrong target or MySQL &lt; 8 | sqlite/mysql only; upgrade MySQL |
| Classic KT data missing after upgrade | Old `plugins/KT/` not imported | `/kt import-kt --dry-run`, then with token |

Reproduce on a test server before raising production log noise.

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

<sub>Built by MonkeyMoon104 — Paper, Inventory Framework, PacketEvents, Flyway, OkHttp, Lamp, Configurate.</sub>

</div>
