# Installation & Setup

## Requirements
- **Java 17** or higher (Java 21 supported)
- **Paper** or **Folia** (Minecraft 1.20.4 – 1.21+)

### Optional Compatible Hooks
- **Vault:** Required for repeated cash payouts and direct faction bank deposits (`FACTION_BANK_DEPOSIT`).
- **PlaceholderAPI:** Required for scoreboard, TAB, and holographic displays using `%outpost_...%` placeholders.
- **Team Plugins:** Outposts automatically detects and bridges with:
  - ZelTeams
  - BetterTeams
  - FactionsUUID / SaberFactions / SavageFactions
  - Towny (Towns)
  - Vanilla Scoreboard Teams (default out-of-the-box fallback)

---

## Installation Steps
1. Download the latest `Outposts-1.0.jar`.
2. Move the `.jar` into your server's `plugins/` directory.
3. Start or restart your server to initialize default configuration files:
   - `plugins/Outposts/config.yml`
   - `plugins/Outposts/schedules.yml`
   - `plugins/Outposts/outposts/south.yml` (demo arena)
4. Configure global options in `config.yml` and set `hooks.team_provider` to `AUTO` or your specific team system.
5. Grant player permissions:
   - `outposts.use`: Granted to default players.
   - `outposts.admin`: Granted to server operators.
