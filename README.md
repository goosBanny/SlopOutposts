# Outposts 🏰

[![Java Version](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Folia-0099FF?style=for-the-badge&logo=minecraft&logoColor=white)](https://papermc.io/)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)
[![Wiki](https://img.shields.io/badge/Documentation-Wiki-blue?style=for-the-badge&logo=gitbook&logoColor=white)](https://github.com/goosBanny/SlopOutposts/wiki)
[![License](https://img.shields.io/badge/License-No--Resale%20%2F%20Open%20Fork-orange?style=for-the-badge)](LICENSE)

**Outposts** is a modern territory control and King-of-the-Hill (KOTH) plugin engineered for competitive Minecraft servers (Factions, HCF, Clans, SkyBlock, and Towny) running on **Paper** and **Folia** (1.20.4 – 1.21+).

Traditional KOTH plugins are static, boring, and easily camped. **Outposts** solves this by turning territory wars into dynamic, scheduled server events with shifting regions, custom capture modes, automated rewards, and ironclad anti-cheese protections.

---

## 💡 What Problems Does Outposts Solve?

- 🚫 **No More Camper Boredom:** Outposts can **dynamically relocate** between different predefined forts and castles across the world. Players track active points using live compasses and visual beacon beams.
- 💰 **Revitalize Server Economies:** Reward holding factions with direct bank deposits, bonus mob drops, increased ShopGUI+ sell rates, and 5-minute milestone bonuses.
- 🛡️ **Zero Bunker Cheating:** Raytraced line-of-sight checks prevent players from hiding inside underground obsidian bunkers. Anti-ally stalling rules prevent friendly factions from faking contests.
- 📅 **Automated Clan Wars:** Built-in cron scheduler automates daily or weekend events. If an outpost is contested when time expires, the **Dynamic Overtime** engine keeps the battle alive until a victor emerges.
- ⚡ **Folia Native & Zero Lag:** Architected with region-aware scheduling, localized chunk ticking, and zero-lock async snapshots for butter-smooth TPS under heavy PvP combat.

---

## ✨ Core Features

- 🎯 **4 Capture Modes:**
  - **Standard Hill:** Classic King of the Hill (0% to 100%). Invaders must knock down defender control before neutralizing.
  - **Tug-of-War:** Symmetrical 50% neutral anchor pulled back and forth in a tug-of-war duel based on live player numbers.
  - **Ticket Accumulation:** Holding the point generates score tickets toward a target victory total.
  - **Passive Decay:** Abandoned, partially captured points bleed back to neutral over time.
- 🔄 **Dynamic Roaming Outposts:** Arenas can shift locations at intervals, on specific timestamps, or immediately upon capture. Includes warmup grace periods and temporary spawn invulnerability.
- 🎁 **Deep Action Pipeline:** Send MiniMessage chat messages, action bars, on-screen titles, sounds, Vault cash, or console commands targeting `CONTROLLER`, `PREVIOUS_CONTROLLER`, `CAPPER`, or `ZONE`. Supports secondary milestone intervals (e.g. `every_seconds: 300`).
- 🎨 **Modern Audio-Visuals:** Distance-culled RGB particle perimeter boundaries, Adventure MiniMessage gradients, and pitch-scaled auditory capture cues culminating in a victory fanfare.
- 🤝 **Universal Hook Support:** Works out-of-the-box with **Vault**, **PlaceholderAPI**, **ZelTeams**, **BetterTeams**, **FactionsUUID / SaberFactions**, and **Towny**.

---

## 🚀 Quick Start (3 Steps)

### 1. Download & Install
1. Download the latest build from the [Releases](https://github.com/goosBanny/SlopOutposts/releases/tag/latest) page.
2. Drop the `.jar` into your server's `plugins/` directory and restart.
3. Outposts will generate `config.yml`, `schedules.yml`, `lang.yml`, and a ready-to-play demo arena at `outposts/south.yml`.

### 2. Create an Outpost with the Visual Wand
1. Type `/outpost wand` to receive the visual selection tool.
2. **Left-click** a block to set Corner 1.
3. **Right-click** a block to set Corner 2. A live particle box will appear outlining your zone.
4. Stand at your desired warp location and run:
   ```bash
   /outpost create desert TEAM STANDARD_HILL
   ```
5. Your new outpost `outposts/desert.yml` is created and ready for action!

---

## 📜 Player & Admin Commands

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/outpost list` | `outposts.use` | View all outposts and live status chips |
| `/outpost info <id>` | `outposts.use` | View detailed controller, progress, and capper counts |
| `/outpost tp <id>` | `outposts.use` | Teleport to an outpost's warp location |
| `/outpost compass <id>` | `outposts.use` | Track active outpost location with your compass |
| `/outpost wand` | `outposts.admin` | Receive the visual setup wand |
| `/outpost create <id> <TEAM\|SOLO> [mode]` | `outposts.admin` | Create an outpost from wand selection |
| `/outpost delete <id>` | `outposts.admin` | Delete an outpost and its configuration |
| `/outpost setwarp <id>` | `outposts.admin` | Set warp coordinates to your current position |
| `/outpost region add <outpost> <id> <name>` | `outposts.admin` | Add a dynamic shifting region from wand selection |
| `/outpost region shift <outpost> [id]` | `outposts.admin` | Force an immediate shift to a region |
| `/outpost forcestart <id> [duration]` | `outposts.admin` | Start an event for duration seconds (default: 30m) |
| `/outpost forcestop <id>` | `outposts.admin` | Immediately reset an outpost back to neutral |
| `/outpost reload` | `outposts.admin` | Non-destructive hot reload of all configs & schedules |
| `/outpost doctor` | `outposts.admin` | Diagnostic audit of Folia regions, hooks, and bounds |

---

## 🧩 PlaceholderAPI Highlights

| Placeholder | Output | Description |
| :--- | :--- | :--- |
| `%outpost_is_active_<id>%` | `true` | Event operational state |
| `%outpost_controller_team_<id>%` | `Vikings` | Name of the controlling faction |
| `%outpost_progress_bar_<id>%` | `██████████░░░░░` | 15-segment progress bar |
| `%outpost_progress_percent_<id>%` | `75.5` | Capture percentage |
| `%outpost_time_remaining_<id>%` | `28:45` | Time remaining in active event window |
| `%outpost_scheduler_next_<id>_formatted%` | `02:15:30` | Countdown to next scheduled start time |

*(See [Placeholders Wiki](https://github.com/goosBanny/SlopOutposts/wiki/Placeholders) for the full 25+ placeholder reference).*

---

## 📖 Documentation Wiki

Visit the [Official Wiki](https://github.com/goosBanny/SlopOutposts/wiki) for deep-dive guides:
- [Arena Configuration](https://github.com/goosBanny/SlopOutposts/wiki/Arena-Configuration)
- [Schedules & Automation](https://github.com/goosBanny/SlopOutposts/wiki/Schedules-&-Automation)
- [Capture Mechanics & Modes](https://github.com/goosBanny/SlopOutposts/wiki/Capture-Mechanics)
- [Actions & Triggers Reference](https://github.com/goosBanny/SlopOutposts/wiki/Actions-&-Triggers)
- [Creating Rewards Cookbook](https://github.com/goosBanny/SlopOutposts/wiki/Creating-Rewards)
- [Anti-Cheese & Fair Play Rules](https://github.com/goosBanny/SlopOutposts/wiki/Anti-Cheese-&-Fair-Play)
- [Developer API & Events](https://github.com/goosBanny/SlopOutposts/wiki/Developer-API)

---

## 📄 License

Outposts is distributed under the **Non-Commercial Resale / Open Fork License**. You are free to fork, customize, and deploy on your servers. **Resale or commercial retail distribution as a paid standalone product is strictly prohibited**. See [LICENSE](LICENSE) for details.
