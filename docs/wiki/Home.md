# Outposts Wiki 🏰

Welcome to the official **Outposts** documentation wiki! Outposts is a territory control and King-of-the-Hill (KOTH) plugin designed for competitive Minecraft servers (Factions, HCF, SkyBlock, Towny, and Clans) running on Paper and Folia (1.20.4 – 1.21+).

---

## 📖 Table of Contents

1. **[Installation & Setup](Installation-&-Setup)**
   - System requirements and dependencies
   - Installation steps
   - Initial configuration overview
2. **[Arena Configuration](Arena-Configuration)**
   - Understanding `outposts/<id>.yml`
   - Bounding box geometry, warps, and visual boundaries
   - Dynamic changing locations & weighted regions
   - Per-outpost language overrides (`lang:`)
3. **[Schedules & Automation](Schedules-&-Automation)**
   - Automated cron event windows (`schedules.yml`)
   - Event duration & Dynamic Overtime engine
   - Pre-event warning broadcasts
   - Scheduler placeholders and strict restart sync
4. **[Capture Mechanics](Capture-Mechanics)**
   - `STANDARD_HILL` (Classic KOTH)
   - `TUG_OF_WAR` (Symmetrical push-pull duel)
   - `TICKET_ACCUMULATION` (Race to victory points)
   - `PASSIVE_DECAY` (Abandonment bleeding)
   - Hysteresis buffers & debounce stabilization
5. **[Actions & Triggers](Actions-&-Triggers)**
   - Trigger lifecycle (`on_capture`, `on_lost`, `on_contest`, `on_tick_reward`)
   - Target selectors (`CONTROLLER`, `PREVIOUS_CONTROLLER`, `CAPPER`, `ZONE`, `GLOBAL`, etc.)
   - Action types (`MESSAGE`, `ACTION_BAR`, `TITLE`, `SOUND`, `COMMAND_CONSOLE`, `TEAM_BANK_DEPOSIT`)
   - Secondary hold intervals (`every_seconds: 300`) and context tokens
6. **[Creating Rewards](Creating-Rewards)**
   - Copy-paste recipes: Faction bank payouts, 5-minute milestones, crate keys, and tactical defense alerts
7. **[Multipliers & Economy](Multipliers-&-Economy)**
   - Mob drop and EXP bonuses
   - PvP combat advantage
   - ShopGUIPlus sell multipliers & comfort perks
8. **[Anti-Cheese & Fair Play](Anti-Cheese-&-Fair-Play)**
   - Raytraced line-of-sight checks
   - Combat damage interruption policies
   - Flight, Elytra, GodMode, and Vanish disqualification
   - Anti-ally stalling
   - Optional regional block & teleport protections
9. **[Commands & Permissions](Commands-&-Permissions)**
   - Player commands (`/outpost list`, `/outpost info`, `/outpost tp`, `/outpost compass`)
   - Admin suite (`/outpost wand`, `/outpost forcestart`, `/outpost doctor`, `/outpost reload`)
   - Permission nodes
10. **[Placeholders](Placeholders)**
    - Complete PlaceholderAPI `%outpost_...%` and `%outpost_scheduler_...%` reference
11. **[Developer API](Developer-API)**
    - API dependencies (Maven/Gradle)
    - Zero-lock snapshot records (`ArenaView`)
    - Custom event listeners
    - Registering custom team roster providers
