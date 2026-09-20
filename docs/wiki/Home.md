# Outposts Wiki 🏰

Welcome to the official **Outposts** documentation wiki! Outposts is a high-performance King-of-the-Hill (KOTH) and territory control plugin designed for competitive Minecraft servers (Factions, HCF, SkyBlock, Towny, and Clans) running on Paper and Folia (1.20.4 – 1.21+).

---

## 📖 Table of Contents

1. **[Installation & Setup](Installation-&-Setup)**
   - System requirements and dependencies
   - Installation steps
   - Initial configuration overview
2. **[Arena Configuration](Arena-Configuration)**
   - Understanding `outposts/<id>.yml`
   - Bounding box geometry and warps
   - Customizing MiniMessage display names
   - Dynamic changing locations & weighted regions
   - Per-outpost language overrides (`lang:`)
3. **[Capture Mechanics](Capture-Mechanics)**
   - `STANDARD_HILL`
   - `TUG_OF_WAR`
   - `TICKET_ACCUMULATION`
   - `PASSIVE_DECAY`
   - Hysteresis buffer & debounce window
   - Relocation activation grace period & spawn protection
4. **[Multipliers & Economy](Multipliers-&-Economy)**
   - Mob drop and EXP bonuses
   - Mob spawner delay acceleration
   - PvP combat advantage
   - Action trigger rewards (console commands, broadcasts, bank deposits)
5. **[Anti-Cheese & Fair Play](Anti-Cheese-&-Fair-Play)**
   - Raytraced line-of-sight checks
   - Combat damage interruption policies
   - Flight, Elytra, GodMode, and Vanish disqualification
   - Anti-ally stalling
   - Optional regional block & teleport protections
6. **[Commands & Permissions](Commands-&-Permissions)**
   - Player commands
   - Admin suite (`/outpost wand`, `/outpost doctor`, `/outpost reload`)
   - Permission nodes
7. **[Placeholders](Placeholders)**
   - Complete PlaceholderAPI `%outpost_...%` reference
8. **[Developer API](Developer-API)**
   - API dependencies (Maven/Gradle)
   - Using `OutpostArena` and `ArenaView` snapshots
   - Custom event listeners
   - Registering custom team roster providers
