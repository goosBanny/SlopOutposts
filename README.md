# Outposts (v1.0.0) 🏰

[![Java Version](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Folia-0099FF?style=for-the-badge&logo=minecraft&logoColor=white)](https://papermc.io/)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)
[![Wiki](https://img.shields.io/badge/Documentation-Wiki-blue?style=for-the-badge&logo=gitbook&logoColor=white)](https://github.com/goosBanny/SlopOutposts/wiki)
[![License](https://img.shields.io/badge/License-No--Resale%20%2F%20Open%20Fork-orange?style=for-the-badge)](LICENSE)

**Outposts** is a competitive King-of-the-Hill (KOTH) and persistent territorial control engine architected for modern Paper and multi-threaded Folia Minecraft servers (1.20.4 – 1.21+).

Built from the ground up on SOLID principles, Outposts combines high-intensity territory wars with zero-allocation spatial math, thread-safe snapshot DTOs, rich audio-visual feedback, and pluggable capture mechanics.

---

## ✨ Features

- 🔄 **Dynamic Changing Outposts:** Outposts can switch locations randomly between predefined weighted regions (interval, scheduled timestamps, or on-capture). Includes activation grace warmup periods, temporary spawn invulnerability on warp/shift, and compass tracking.
- 📅 **Automated Schedules & Overtime Engine:** Built-in cron scheduler (`schedules.yml`) with automated activation windows, warning alerts, and dynamic overtime if an outpost is contested when time expires.
- ⚡ **Multi-Threaded Folia & Paper Engine:** Anchor chunk-localized ticking, thread-safe `EntityScheduler` player routing, and asynchronous DTO snapshots guaranteeing zero lock contention.
- 📐 **Zero-Allocation Spatial Math:** Uses FastUtil primitive 64-bit chunk bitwise hashing `((long)x & 0xFFFFFFFFL) | (((long)z & 0xFFFFFFFFL) << 32)` and primitive AABB evaluation without allocating Bukkit `Location` objects.
- 🎯 **4 Pluggable Capture Mechanics:**
  - **Standard Hill (`STANDARD_HILL`):** Classic KOTH (0% -> 100%). Invading forces must knock the defending bar to 0% before neutralizing.
  - **Tug-of-War (`TUG_OF_WAR`):** Symmetrical 50% neutral anchor pulled toward extremes based on real-time capper ratios.
  - **Ticket Accumulation (`TICKET_ACCUMULATION`):** Holding the pad accumulates tickets toward a target victory score.
  - **Passive Decay (`PASSIVE_DECAY`):** Unfinished captures bleed progress back to 0% when vacated.
- 🛡️ **Anti-Cheese & Fair Play Rules:**
  - **Raytraced Line-of-Sight:** Emits eye-to-center raytraces filtering non-occluding blocks (vines, signs, banners).
  - **Combat Interruption Policy:** Configurable reaction to taking PvP damage (`IGNORE`, `PAUSE_CAPTURE`, `RESET_PROGRESS`).
  - **Anti-Ally Stalling:** Queries the team provider's alliance graph to prevent friendly factions from artificially contesting pads.
  - **Invulnerability & Stealth Checks:** Disqualifies players in Flight, Elytra glide, GodMode, Warp Protection, or Vanish.
- 🎨 **Rich Audio-Visual Feedback & Per-Outpost Localization:**
  - **Adventure Chromatic BossBars & ActionBars:** Real-time color transitions with custom per-outpost language overrides.
  - **Pitch-Scaled Audio:** Auditory cues scale in pitch from 0.5 to 2.0 as capture approaches 100%, culminating in a victory fanfare.
  - **Perimeter Displays:** Distance-culled RGB dust particles outlining the 12 edges of the arena bounding box.
- 💎 **Passive & Active Multipliers:**
  - **Mob Spawner Boost:** Accelerates tick delay on spawners in held territories.
  - **Mob Drop & EXP Multipliers:** Multiplies item drops and experience orbs from hostile mob kills.
  - **PvP Combat Advantage:** Percentage damage boost dealt by controlling faction members.
  - **ShopGUI+ Multiplier Hook:** Configurable bonus sell rate for active outpost controllers.
  - **Comfort Perks:** Disables hunger depletion (`NO_HUNGER_LOSS`) for controlling members.
- 🤝 **Decoupled Team & Economy Hooks:**
  - First-class support for **ZelTeams**, **BetterTeams**, **FactionsUUID / SaberFactions**, **Towny**, and **Vanilla Scoreboard Teams**.
  - Direct compile-time team banking via **ZelTeams**, **BetterTeams**, and **Vault**.
- 🛠️ **Administrative Suite:**
  - Visual setup wand (`/outpost wand`) with live RGB particle selection box.
  - In-game region management (`/outpost region <add|remove|list|setweight|shift>`).
  - System Doctor (`/outpost doctor`) for Folia regional threading, hook, and bounding box overlap diagnostics.
  - Non-destructive hot reload (`/outpost reload`).

---

## 🚀 Quick Start

### 1. Requirements
- **Java 17** or higher
- **Paper** or **Folia** (1.20.4 – 1.21+)
- *(Optional)* [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/), [Vault](https://www.spigotmc.org/resources/vault.34315/), [BetterTeams](https://www.spigotmc.org/resources/better-teams.17129/), [ShopGUI+](https://www.spigotmc.org/resources/shopgui.6515/)

### 2. Installation
1. Download the latest `Outposts-1.0.jar` from the [Releases](https://github.com/goosBanny/SlopOutposts/releases/tag/latest) page.
2. Place the jar into your server's `plugins/` directory.
3. Start or restart your server.
4. Outposts will automatically generate `config.yml`, `schedules.yml`, `lang.yml`, and a default demo arena at `outposts/south.yml`.

### 3. Creating Your First Outpost
1. Run `/outpost wand` to receive the visual selection wand.
2. **Left-click** a block to select Corner 1.
3. **Right-click** a block to select Corner 2. A live pink particle box will appear outlining the selection.
4. Stand at the desired teleport point and type:
   ```bash
   /outpost create desert
   ```
5. Your new arena `outposts/desert.yml` is saved and immediately active!

---

## 📜 Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/outpost list` | `outposts.use` | Lists all arenas with real-time status chips |
| `/outpost info <id>` | `outposts.use` | Displays detailed status, controllers, and bounds |
| `/outpost tp <id>` | `outposts.use` | Teleports to an outpost's configured warp location |
| `/outpost compass <id>` | `outposts.use` | Points your compass directly to the active outpost location |
| `/outpost shop` | `outposts.use` | Displays outpost black-market shop information |
| `/outpost wand` | `outposts.admin` | Gives the visual boundary setup wand |
| `/outpost create <id>` | `outposts.admin` | Creates an outpost arena from active wand selection |
| `/outpost delete <id>` | `outposts.admin` | Deletes an outpost arena and removes its config |
| `/outpost setwarp <id>` | `outposts.admin` | Sets the warp location to your current position |
| `/outpost region add <outpost> <reg_id> <name> [wt]` | `outposts.admin` | Adds a dynamic location region using current wand selection |
| `/outpost region remove <outpost> <reg_id>` | `outposts.admin` | Removes a dynamic region from an outpost |
| `/outpost region list <outpost>` | `outposts.admin` | Lists all configured shifting regions and weights |
| `/outpost region setweight <outpost> <reg_id> <wt>` | `outposts.admin` | Updates the random switch weight of a region |
| `/outpost region shift <outpost> [reg_id]` | `outposts.admin` | Forces an immediate shift to a random or specific region |
| `/outpost reload` | `outposts.admin` | Hot-reloads all configs without interrupting captures |
| `/outpost doctor` | `outposts.admin` | Audits threading, hooks, and overlapping bounding boxes |
| `/outpost forcestart <id>` | `outposts.admin` | Force unlocks and begins an outpost capture cycle |
| `/outpost forcestop <id>` | `outposts.admin` | Forces an outpost back to neutral |

---

## 🧩 PlaceholderAPI Expansions

Outposts registers the `%outpost_<...>%` placeholder identifier:

| Placeholder | Example Output | Description |
| :--- | :--- | :--- |
| `%outpost_next_event_time%` | `1h 24m` / `Active` | Schedule countdown to the next scheduled event |
| `%outpost_is_active_<id>%` | `true` | Arena operational state |
| `%outpost_is_contested_<id>%` | `false` | True if rival factions contest the pad |
| `%outpost_is_locked_<id>%` | `false` | True if in post-capture lockout |
| `%outpost_state_formatted_<id>%` | `Controlled` | Capitalized status chip |
| `%outpost_progress_bar_<id>%` | `██████████░░░░░` | Formatted segmented progress bar |
| `%outpost_progress_percent_<id>%` | `75.5` | Floating-point percentage |
| `%outpost_controller_team_<id>%` | `Vikings` | Name of controlling faction |
| `%outpost_capping_team_<id>%` | `Spartans` | Name of active capturing faction |
| `%outpost_capper_count_<id>%` | `3` | Number of cappers inside the pad |
| `%outpost_time_controlled_<id>%` | `01:45:20` | Formatted holding duration |
| `%outpost_lock_remaining_<id>%` | `04:15` | Remaining lockout countdown |
| `%outpost_<id>_coords%` | `105, 65, 105` | Formatted center coordinates of active region |
| `%outpost_<id>_x%` / `%y%` / `%z%` | `105` / `65` / `105` | Individual coordinate integers |
| `%outpost_<id>_current_region_name%` | `South Docks` | Display name of currently active region |
| `%outpost_<id>_next_shift_seconds%` | `240` | Seconds remaining until the next relocation |
| `%outpost_<id>_next_shift_formatted%`| `04:00` | Formatted countdown until next relocation |
| `%outpost_<id>_is_shifting%` | `true` | True if dynamic locations are enabled |
| `%outpost_<id>_is_warming_up%` | `false` | True if in relocation activation grace period |
| `%outpost_<id>_warmup_remaining%` | `12` | Seconds remaining on relocation activation grace |
| `%outpost_player_inside%` | `true` | True if viewer stands inside any outpost |
| `%outpost_player_zone%` | `south` | ID of outpost viewer currently occupies |
| `%outpost_team_outpost_count%` | `2` | Number of outposts viewer's team controls |

---

## 🏗️ Architecture & Development

Outposts implements the SOLID architectural hierarchy:
- **`ArenaDescriptor` (ISP):** Immutable configuration and geometry definition.
- **`ArenaViewSnapshot` (DTO):** Immutable Java 17 record snapshot exposed safely to async threads (PAPI, Discord, GUIs).
- **`OutpostArena`:** Package-private ticking core running spatial collision math and hysteresis validation.
- **`FoliaCompatScheduler`:** Dynamic abstraction layer supporting both single-threaded Bukkit/Paper and multi-threaded Folia region schedulers.

### Compiling from Source
```bash
git clone https://github.com/goosBanny/Outposts.git
cd Outposts
./gradlew build
```
Compiled jar will be located in `build/libs/Outposts-1.0.jar`.

---

## 📄 License

Outposts is distributed under the **Non-Commercial Resale / Open Fork License**. You are free to fork, modify, deploy on servers, and contribute to this repository. However, **resale or commercial retail distribution as a paid standalone plugin is strictly prohibited**. See [LICENSE](LICENSE) for details.
