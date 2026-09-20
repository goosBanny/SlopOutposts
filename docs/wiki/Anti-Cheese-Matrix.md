# Anti-Cheese Matrix

Outposts features a competitive anti-cheese matrix ensuring captures require genuine physical presence and fair tactical engagement.

---

## Matrix Features

### 1. Raytraced Line-of-Sight (`line_of_sight: true`)
- Casts a raytrace from the player's eye level to the geometric center of the capture pad.
- **Occlusion Filtering:** Only solid occluding blocks (e.g., stone, obsidian, iron blocks) block line-of-sight. Passable and decorative blocks (vines, signs, banners, tall grass) are intelligently ignored.
- **Prevents:** Hiding inside subterranean obsidian bunkers or under trapdoors while capturing.

### 2. Combat Interruption Policy (`combat_damage_policy`)
Configures the reaction when a player takes PvP combat damage while inside the capture zone:
- `IGNORE`: No effect on capture.
- `PAUSE_CAPTURE`: Pauses contribution for `combat_pause_seconds` (default: 5s).
- `RESET_PROGRESS`: Instantly resets the accumulated capture percentage upon taking damage.

### 3. Anti-Ally Stalling (`disallow_allied_stall: true`)
- Queries the team provider's alliance relation graph.
- If Team A is allied with Team B, Team B cannot step onto the pad to trigger artificial `CONTESTED` status and stall out rival invaders on behalf of Team A.

### 4. Movement & Stealth Disqualifications
- `disallow_godmode`: Disqualifies players who have invulnerability or godmode enabled (via Essentials, CMI, WorldGuard, or vanilla).
- `disallow_flying`: Disqualifies players who are in creative or survival flight.
- `disallow_elytra`: Disqualifies players who are gliding with an Elytra.
- `disallow_vanished`: Disqualifies players in vanish or stealth modes.

### 5. Territorial Command & Block Protection
Configured in `config.yml` under `anti_lag`:
- **`blocked_commands`**: Prevents teleportation commands (`/spawn`, `/home`, `/tpa`, `/warp`) while inside an outpost zone.
- **`prevent_block_break` / `prevent_block_place`**: Prevents griefing, suffocating enemies, or barricading the pad with cobwebs or obsidian.
- **`prevent_chorus_fruit`**: Prevents glitch-teleporting inside walls via chorus fruit.
