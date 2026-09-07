# KTPlus

**Premium kill-effects plugin for Paper 1.21+** — customizable shop GUI, persistent unlocks, multi-database storage, Folia-ready scheduling, and deep integrations with Vault, LuckPerms, WorldGuard, and PlaceholderAPI.

[![Version](https://img.shields.io/badge/version-4.0.0-blue.svg)](https://github.com/MonkeyMoon104/KTPlus)
[![API](https://img.shields.io/badge/Paper-1.21%2B%20%7C%2026.x-brightgreen.svg)](https://papermc.io/)
[![Folia](https://img.shields.io/badge/Folia-supported-success.svg)](https://papermc.io/software/folia)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-see%20repository-lightgrey.svg)](https://github.com/MonkeyMoon104/KTPlus)
[![Spigot](https://img.shields.io/badge/Spigot-125998-yellow.svg)](https://www.spigotmc.org/resources/125998/)

---

## Table of contents

- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Supported versions](#supported-versions)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [Economy](#economy)
- [Database](#database)
- [Review rewards](#review-rewards)
- [Integrations](#integrations)
- [PlaceholderAPI](#placeholderapi)
- [Building from source](#building-from-source)
- [Architecture](#architecture)
- [Development](#development)
- [Troubleshooting](#troubleshooting)
- [Links](#links)

---

## Overview

KTPlus is a production-oriented Paper plugin that lets players unlock and activate **kill effects** — cinematic sequences that play when they eliminate another player (or, depending on configuration, mobs). Effects are browsed and purchased through a fully configurable chest GUI, balances are stored in a real database, and the plugin is designed to run cleanly on both Paper and Folia.

| | |
|---|---|
| **Author** | MonkeyMoon104 |
| **Version** | 4.0.0 |
| **Main class** | `com.monkey.ktplus.KTPlus` |
| **Commands** | `/ktplus`, `/kt`, `/killeffect` |
| **Spigot resource** | [125998](https://www.spigotmc.org/resources/125998/) |
| **Repository** | [github.com/MonkeyMoon104/KTPlus](https://github.com/MonkeyMoon104/KTPlus) |
| **Default branch** | `enhanced` |

---

## Features

### Kill effects

- **71 built-in effects** across rarity tiers (`common` → `ultra`)
- Per-effect pricing, premium pricing, damage rules, and perk settings
- Runtime session management with temporary blocks, particles, entities, and schematics
- Admin preview via `/kt test <effect>`

### Player experience

- Configurable **shop / selector GUI** (categories, scrolling, filters, balance display)
- Inventory Framework backend with vanilla inventory fallback
- Sound feedback for GUI clicks and review reward completion
- Optional **resource pack** push on join (custom sounds for selected effects)

### Economy & progression

- Built-in **KillCoins** economy (default) or **Vault** bridge
- Kill rewards for players and mobs
- Persistent unlocks / purchases
- Optional LuckPerms permission grant on purchase

### Storage & ops

- **SQLite**, **MySQL**, **MariaDB**, and **PostgreSQL**
- **Flyway** schema migrations (Java migrations, Paper classloader–safe)
- In-game **database migration** tool (`sqlite` ↔ `mysql`)
- Branded boot logging with effect/storage summaries

### Platform & integrations

- **Folia-supported** (`folia-supported: true`) with region-aware scheduling
- Soft-depends: Vault, LuckPerms, WorldGuard, PlaceholderAPI
- WorldGuard region respect / bypass for block-changing effects
- Spigot **update checker**
- **GitHub / SpigotMC review rewards** with timed verification polling
- **bStats** metrics (shaded)
- **PacketEvents** loaded at runtime via a dedicated library loader

---

## Requirements

### Server

- **Paper** 1.21.1+ (or Paper **26.x**)
- Folia is supported on compatible builds

### Optional plugins

| Plugin | Purpose |
|--------|---------|
| [Vault](https://www.spigotmc.org/resources/vault.243/) | External economy provider |
| [LuckPerms](https://luckperms.net/) | Permission grants on purchase |
| [WorldGuard](https://enginehub.org/worldguard) | Region protection for effect block changes |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Placeholder expansion |

### Build (developers)

| Tool | Version |
|------|---------|
| JDK (main / core) | **21** |
| JDK (NMS 26.x modules) | **25** (toolchain) |
| Gradle | **9.7.1** (wrapper included) |

---

## Supported versions

KTPlus ships version-specific NMS bridges and selects the correct one at runtime:

| Server version | NMS module |
|----------------|------------|
| 1.21 / 1.21.1 | `v1_21_1` |
| 1.21.3 | `v1_21_3` |
| 1.21.4 | `v1_21_4` |
| 1.21.5 | `v1_21_5` |
| 1.21.6 | `v1_21_6` |
| 1.21.7 | `v1_21_7` |
| 1.21.8 | `v1_21_8` |
| 1.21.9 | `v1_21_9` |
| 1.21.10 | `v1_21_10` |
| 1.21.11 | `v1_21_11` |
| 26.1.x | `v26_1` |
| 26.2.x (and other 26.x) | `v26_2` |

`plugin.yml` declares `api-version: '1.21'`.

---

## Installation

1. Download `KTPlus-4.0.0.jar` from [Releases](https://github.com/MonkeyMoon104/KTPlus/releases) or build it (see [Building from source](#building-from-source)).
2. Place the jar in your server `plugins/` folder.
3. Start the server once to generate configuration files.
4. Stop the server (recommended) and edit configs under `plugins/KTPlus/`.
5. Start again.

> **Note:** Paper will download declared Maven libraries on first load (Jackson, HikariCP, Flyway, OkHttp, JDBC drivers, Lamp, Configurate, Caffeine, …). PacketEvents is prepared by KTPlus’ own runtime library loader.

---

## Quick start

```text
1. Install KTPlus and restart
2. Give yourself coins:          /kt killcoins add <player> 1000
3. Open the GUI:                 /kt
4. Buy / select an effect
5. Kill a player to trigger it
```

Reload configs without a full restart:

```text
/kt reload
```

---

## Commands

Primary command: **`/ktplus`**  
Aliases: **`/kt`**, **`/killeffect`**

| Command | Description | Typical permission |
|---------|-------------|--------------------|
| `/kt` | Open the effects GUI | (player) |
| `/kt reload` | Reload configuration | `ktplus.reload` |
| `/kt set <effect> [player]` | Select an effect | `ktplus.set` (+ `ktplus.set.others`) |
| `/kt clear [player]` | Clear the selected effect | `ktplus.clear` (+ `ktplus.clear.others`) |
| `/kt test <effect>` | Preview an effect at your location | `ktplus.test` |
| `/kt killcoins bal [player]` | Show balance | `ktplus.killcoins` |
| `/kt killcoins add \| take \| set \| reset <player> <amount>` | Admin economy ops | `ktplus.killcoins.admin` |
| `/kt review <github\|spigotmc> <account>` | Claim review / star rewards | (player) |
| `/kt migrate <sqlite\|mysql> [flags] <token>` | Migrate database | `ktplus.migrate` |

### Migration flags (admin)

```text
/kt migrate <sqlite|mysql> [--dry-run] [--include-temp-blocks]
           [--force-pending-inventory] [--allow-nonempty-target]
           <confirmation-token>
```

In-game migration currently supports **SQLite ↔ MySQL** only (see [Database](#database)).

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `ktplus.admin` | op | Administrative access |
| `ktplus.admin.bypass` | op | Bypass restrictions |
| `ktplus.reload` | op | Reload configs |
| `ktplus.migrate` | op | Run DB migration |
| `ktplus.set` | true | Select own effect |
| `ktplus.set.others` | op | Select effect for others |
| `ktplus.clear` | true | Clear own effect |
| `ktplus.clear.others` | op | Clear effect for others |
| `ktplus.test` | op | Preview effects |
| `ktplus.killcoins` | true | View balances |
| `ktplus.killcoins.admin` | op | Modify balances |
| `ktplus.worldguard.bypass` | op | Bypass WorldGuard checks for effects |
| `ktplus.<effectId>.use` | — | Per-effect usage (granted / checked by access layer) |

---

## Configuration

All files are generated under `plugins/KTPlus/` from the plugin resources:

| File | Purpose |
|------|---------|
| `config.yml` | Global settings, disabled worlds, WorldGuard / LuckPerms options, schematic defaults |
| `messages.yml` | Player-facing message templates |
| `effects.yml` | Effect catalog (categories, prices, perks, damage) |
| `economy.yml` | Economy provider, starting balance, kill rewards, currency labels |
| `gui.yml` | GUI layout, categories, buttons, item lore |
| `database.yml` | Database type, credentials, pool size |
| `events.yml` | Optional random server events |
| `resource-pack.yml` | Pack URL / hash / UUID / required flag + sound keys |
| `performance.yml` | Session limits, cooldowns, adaptive TPS throttling |

After editing, run `/kt reload` or restart the server.

---

## Economy

Configured in `economy.yml`:

```yaml
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

- **`KILLCOINS`** — internal balance stored in the plugin database  
- **`VAULT`** — requires Vault + an economy plugin  

Balances can be managed with `/kt killcoins …`.

---

## Database

Configured in `database.yml`:

```yaml
database:
  type: "sqlite"          # sqlite | mysql | mariadb | postgresql
  file: "ktplus.db"       # SQLite only
  host: "localhost"
  port: 3306
  database: "ktplus"
  username: "root"
  password: ""
  pool-size: 5            # forced to 1 for SQLite
```

| Engine | Runtime | Flyway | In-game migrate |
|--------|---------|--------|-----------------|
| SQLite | ✅ (default) | ✅ | ✅ ↔ MySQL |
| MySQL | ✅ (8.0+) | ✅ | ✅ ↔ SQLite |
| MariaDB | ✅ | ✅ | ❌ use external tools |
| PostgreSQL | ✅ | ✅ | ❌ use external tools |

Schema is applied automatically via **Flyway** Java migrations on startup.

---

## Review rewards

Players can earn KillCoins by verifying a GitHub star or SpigotMC review.

```text
/kt review github <github-username>
/kt review spigotmc <spigot-username>
```

### Rules

| Platform | Requirement | Reward |
|----------|-------------|--------|
| GitHub | Star [MonkeyMoon104/KTPlus](https://github.com/MonkeyMoon104/KTPlus) | **500** coins |
| SpigotMC | Leave a **4★+** review on resource [125998](https://www.spigotmc.org/resources/125998/) | **100** coins × stars (max 5★ → 500) |

### Flow

1. Player starts a session with their **platform username** (not the Minecraft IGN).
2. KTPlus checks presence asynchronously and starts a **300s** verification window.
3. Every **15s** it polls until the star/review is detected (or the timer expires).
4. On success: claim is stored, coins are granted, objective sound plays for the player, and other online players receive a clickable teaser (with exp-orb sound).
5. Already-claimed players/accounts cannot claim again.

> Already-claimed responses only show the claim message — no extra pitch/tutorial spam.

---

## Integrations

### WorldGuard

- Soft-depend; configurable respect / bypass modes in `config.yml`
- Permission `ktplus.worldguard.bypass` for staff

### LuckPerms

- Optional automatic permission grant when a player purchases an effect  
- Controlled under `luckperms` in `config.yml`

### Vault

- Set `economy.provider: VAULT` to route purchases and balances through Vault

### Resource pack

- Configured in `resource-pack.yml`  
- Can push on join with SHA-1 verification and required/optional modes  
- Used for custom effect sounds (e.g. sniper, aura farming)

---

## PlaceholderAPI

Expansion identifier: **`ktplus`** (derived from the plugin name).

### Global

| Placeholder | Description |
|-------------|-------------|
| `%ktplus_version%` | Plugin version |
| `%ktplus_name%` / `%ktplus_plugin%` | Plugin name |
| `%ktplus_author%` | Authors |
| `%ktplus_provider%` | Economy provider id |
| `%ktplus_economy%` | Economy enabled (`yes`/`no`) |
| `%ktplus_effect_count%` | Total registered effects |

### Player

| Placeholder | Description |
|-------------|-------------|
| `%ktplus_balance%` / `%ktplus_coins%` | Balance |
| `%ktplus_balance_formatted%` | Formatted balance |
| `%ktplus_selected%` / `%ktplus_effect%` | Selected effect id |
| `%ktplus_selected_name%` | Selected effect display name |
| `%ktplus_selected_price%` | Selected effect price |
| `%ktplus_selected_category%` | Selected category |
| `%ktplus_has_selected%` | Whether an effect is selected |
| `%ktplus_unlocked_count%` | Owned / unlocked count |
| `%ktplus_locked_count%` | Remaining locked count |
| `%ktplus_owns_<effectId>%` | Owns effect (`yes`/`no`) |
| `%ktplus_unlocked_<effectId>%` | Can activate effect (`yes`/`no`) |

---

## Building from source

### Clone

```bash
git clone https://github.com/MonkeyMoon104/KTPlus.git
cd KTPlus
git checkout enhanced
```

### Build

```bash
# Linux / macOS
./gradlew build

# Windows
gradlew.bat build
```

Output jar:

```text
dist/build/libs/KTPlus-4.0.0.jar
```

### Useful tasks

| Task | Description |
|------|-------------|
| `./gradlew build` | Full build (includes `:dist`) |
| `./gradlew test` | Run unit tests (`:core:test`) |
| `./gradlew :dist:shadowJar` | Produce the shaded plugin jar |
| `./gradlew :dist:verifyPluginJar` | Verify relocations & library descriptors |

### Notes for contributors

- Main modules compile with **Java 21**.
- Paper **26.x** NMS modules use a **Java 25** toolchain.
- A local Maven vendor repo under `build-logic/repo` supplies temporarily patched **paperweight** and **Inventory Framework** builds until upstream merges the fixes. See [`build-logic/repo/README.md`](build-logic/repo/README.md).

---

## Architecture

```text
KTPlus/
├── build-logic/          # Gradle convention plugins + vendor Maven repo
├── common/               # Shared platform / GUI capability contracts
├── nms-bridge/           # Version → NMS module mapping & bridge API
├── NMS/
│   ├── v1_21_1 … v1_21_11   # Paper 1.21.x adapters (reobf)
│   ├── v26_1 / v26_2        # Paper 26.x adapters (Mojmap)
├── core/                 # Plugin logic, configs, effects, GUI, storage
├── dist/                 # Shadowed distributable jar
├── gradle/               # Version catalog + wrapper
└── resourcepacks/        # Bundled pack assets
```

### Runtime library strategy

| Kind | Mechanism | Examples |
|------|-----------|----------|
| Maven Central libraries | Paper `plugin.yml` `libraries:` | Jackson, HikariCP, Flyway, OkHttp, JDBC, Lamp |
| Shaded into the jar | Shadow + relocate under `com.monkey.ktplus.libs.*` | Inventory Framework, bStats, ASM, Querz NBT, jar-relocator |
| Custom loader | Descriptor + download/relocate on `onLoad` | PacketEvents |

---

## Development

### Branching

- Default branch: **`enhanced`**
- Branch protection: force-push and deletion are disabled

### Dependency updates

Dependabot runs **weekly** (Mondays, Europe/Rome) against `enhanced`:

- Gradle version catalog updates (grouped for Jackson, Flyway, JUnit, OkHttp, Gradle plugins)
- GitHub Actions updates (when workflows exist)
- Vendored / Paper pin lines are ignored (see `.github/dependabot.yml`)

Review Dependabot PRs, merge when green, then `git pull` locally.

### Upstream patches tracked by this repo

| Library | Issue | Status |
|---------|-------|--------|
| paperweight-userdev | Gradle 10 `by registering` deprecation | Vendored + [PR #405](https://github.com/PaperMC/paperweight/pull/405) |
| Inventory Framework 0.12.1 | `ArrayIndexOutOfBoundsException` in `processMethodAnnotations` | Vendored + [PR #2553](https://github.com/stefvanschie/IF/pull/2553) |

---

## Troubleshooting

| Symptom | Likely cause | What to try |
|---------|--------------|-------------|
| Plugin disables on load | PacketEvents download/relocate failed | Check console for `[Library]` errors and network access to Maven mirrors |
| `Library GUI unavailable` | Inventory Framework failure | Ensure you run a build that includes the patched IF vendor artifact |
| Effects don’t play | Wrong permission / locked effect / disabled world | Check `ktplus.<id>.use`, GUI unlock state, `config.yml` disabled worlds |
| Vault balances don’t move | Provider still `KILLCOINS` | Set `economy.provider: VAULT` and install Vault + an economy plugin |
| Review says already claimed | Player or platform account already used | Expected; one claim per player/platform account |
| Migration rejected | Target not sqlite/mysql or MySQL &lt; 8 | Use supported engines; upgrade MySQL |

Enable verbose logs carefully on production; prefer reproducing on a test server first.

---

## Links

- **GitHub:** https://github.com/MonkeyMoon104/KTPlus  
- **SpigotMC:** https://www.spigotmc.org/resources/125998/  
- **Paper docs:** https://docs.papermc.io/  
- **Issues:** https://github.com/MonkeyMoon104/KTPlus/issues  

---

## Credits

Developed by **MonkeyMoon104**.

Built on Paper, with gratitude to the maintainers of Inventory Framework, PacketEvents, Flyway, OkHttp, Lamp, Configurate, and the wider Minecraft plugin ecosystem.

---

<p align="center">
  <sub>KTPlus Enhanced · Paper kill effects, done properly.</sub>
</p>
