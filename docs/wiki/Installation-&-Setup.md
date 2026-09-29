# Installation & Setup 🚀

## Requirements
- **Java 17** or higher (Java 21 recommended)
- **Paper** or **Folia** (Minecraft 1.20.4 – 1.21+)

### Optional Compatible Hooks
- **Vault:** Required for cash payouts and faction bank deposits.
- **PlaceholderAPI:** Required for scoreboard, TAB, and holographic displays using `%outpost_...%` placeholders.
- **Team Plugins:** Outposts automatically detects and bridges with:
  - ZelTeams
  - BetterTeams
  - FactionsUUID / SaberFactions / SavageFactions
  - Towny (Towns)
  - Vanilla Scoreboard Teams (default out-of-the-box fallback)

---

## Installation Steps
1. Download the latest build from the [Releases](https://github.com/goosBanny/SlopOutposts/releases/tag/latest) page.
2. Place the `.jar` into your server's `plugins/` directory.
3. Start or restart your server to initialize default configuration files:
   - `plugins/Outposts/config.yml` (Global server options & anti-cheese)
   - `plugins/Outposts/schedules.yml` (Automated cron event schedules)
   - `plugins/Outposts/lang.yml` (MiniMessage localization strings & prefix)
   - `plugins/Outposts/outposts/south.yml` (Ready-to-use demo arena)
4. Configure global options in `config.yml` and set `hooks.team_provider` to `AUTO` or your specific team system.
5. Grant player permissions:
   - `outposts.use`: Granted to default players (true by default).
   - `outposts.admin`: Granted to server operators.
   - `outposts.admin.bypass`: Allows admins to bypass block break/place protections in outpost zones.
