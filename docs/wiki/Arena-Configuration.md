# Arena Configuration

Every outpost on your server is stored in its own native YAML file located under `plugins/Outposts/outposts/<id>.yml`.

---

## Anatomy of an Outpost Configuration

```yaml
id: "south"

meta:
  name: "<gradient:#FF416C:#8A2387><bold>South Outpost</bold></gradient>"
  icon: "NETHERITE_SWORD"

geometry:
  world: "world"
  min: { x: 362, y: 23, z: -1144 }
  max: { x: 368, y: 27, z: -1138 }
  warp: { x: 365.5, y: 28.0, z: -1141.5, yaw: 0.0, pitch: 0.0 }

mechanics:
  enabled: true
  occupancy_mode: "TEAM" # TEAM or SOLO (free-for-all mode)
  mode: "STANDARD_HILL" # STANDARD_HILL, TUG_OF_WAR, TICKET_ACCUMULATION, PASSIVE_DECAY
  speed:
    percent_per_second: 2.5
    uncapture_percent_per_second: 2.5
    scaling_per_member: 0.5
    max_cappers_counted: 4
  behavior:
    freeze_when_contested: true
    lose_control_threshold: 100.0
    lockout_seconds: 10
    knock_delay_seconds: 5
    passive_decay:
      enabled: true
      rate_per_second: 2.5
    hysteresis_buffer_percent: 2.0
    state_change_cooldown_seconds: 1.0

multipliers:
  spawner_rate: 1.5
  mob_drop_rate: 1.75
  exp_drop_rate: 2.0
  damage_rate: 1.10
  shopgui_sell_rate: 1.35

actions:
  on_capture:
    - type: "BROADCAST"
      message: "<prefix> <gold><team></gold> captured <name>!"
    - type: "COMMAND_CONSOLE_PER_PLAYER"
      command: "eco give %player% 5000"
    - type: "TEAM_BANK_DEPOSIT"
      amount: 1000.0
  on_lost:
    - type: "BROADCAST"
      message: "<prefix> <gold><team></gold> lost control of <name>!"
```

---

## Section Breakdown

### 1. `meta`
- **`name`**: Display name parsed with Adventure MiniMessage format (gradients, hex colors, bold/italic).
- **`icon`**: Material name used when rendering GUI icons.

### 2. `geometry`
- **`world`**: Bukkit world identifier.
- **`min` / `max`**: Integer cuboid corners defining the capture boundary.
- **`warp`**: Teleport destination coordinates used by `/outpost tp <id>`.

### 3. `mechanics`
- **`occupancy_mode`**: `TEAM` (controlled by whole clans/factions) or `SOLO` (individual player free-for-all).
- **`percent_per_second`**: Base capture percentage change per second (e.g. 2.5% = 40s base capture time).
- **`uncapture_percent_per_second`**: Base percentage deducted per second when an invading team knocks down enemy progress (100% -> 0%).
- **`scaling_per_member`**: Bonus percentage added for each additional teammate standing inside the zone.
- **`max_cappers_counted`**: Upper limit of teammates counted for speed calculation.
- **`lockout_seconds`**: Cooldown (in seconds) after a successful capture during which the arena remains locked (default: 10s).
- **`knock_delay_seconds`**: Grace duration (in seconds) before an invader can begin reducing defender progress.
- **`passive_decay.rate_per_second`**: Bleed rate (in percent per second) deducted when an unfinished point is abandoned.
- **`lose_control_threshold`**: Percentage at which the defending team officially loses ownership of the outpost. Default is `100.0` (attackers must knock defenders all the way down from 100% to 0% to take it). If set to `50.0`, attackers only need to push defenders down to 50% to neutralize the point.
- **`hysteresis_buffer_percent`**: Buffer zone to prevent control status from flickering on/off if progress bounces near 100%. For example, with `2.0%`, defenders maintain control until their bar drops below `98.0%`.
- **`state_change_cooldown_seconds`**: Stabilization delay (in seconds) before transitioning between active and uncontested states.

### 4. `mode_settings` (Per-Mode Configuration)
Specific fine-tuning parameters for each game mode:
- **`standard_hill`**:
  - `min_cappers_required`: Minimum valid players required inside the zone to begin capture (default: `1`).
- **`tug_of_war`**:
  - `neutral_anchor_percent`: Midpoint anchor where uncaptured pads start (default: `50.0%`).
  - `contested_advantage_scaling`: Pull speed multiplier per surplus capper (default: `0.5`).
  - `neutral_drift_rate`: Percentage per second progress drifts toward 50.0% when abandoned (default: `2.5%/s`).
  - `team_assignment`: `FIRST_TWO_FACTIONS` (symmetrical 2-faction duel, 3rd parties ignored) or `AUTO_RED_BLUE` (balances players into Red vs Blue).
  - `deadzone_buffer_percent`: Neutral deadlock range around midpoint (default: `2.5%`, 47.5% - 52.5%).
- **`ticket_accumulation`**:
  - `target_tickets`: Score required for victory (default: `1000`).
  - `tickets_per_second`: Tickets generated per second while uncontested (default: `10.0`).

### 5. `actions`
- **`COMMAND_CONSOLE_PER_PLAYER`**: Dispatches console command for each online member of the controlling team (or the winning solo player).
- **`COMMAND_CONSOLE_PER_TEAM`**: Dispatches console command once for the team/console.
- **`COMMAND_CONSOLE`**: Standard command execution with optional `target: "PLAYER" | "LEADER" | "TEAM" | "ZONE" | "TEAM_ZONE"`.
- **`TEAM_BANK_DEPOSIT`**: Directly deposits into team bank (ZelTeams / Vault / BetterTeams) or player account in SOLO mode.

---

## Dynamic Changing Locations (`dynamic_locations`)

Outposts can dynamically shift between a list of pre-configured regions at runtime:

```yaml
dynamic_locations:
  enabled: true
  switch_mode: "INTERVAL"           # INTERVAL | SCHEDULED_TIMESTAMPS | RANDOM_INTERVAL | ON_CAPTURE
  switch_interval_seconds: 300      # Relocation frequency in seconds
  scheduled_timestamps: []          # Second marks for SCHEDULED_TIMESTAMPS mode (e.g. [600, 1200])
  random_interval_min: 180          # Minimum random interval seconds
  random_interval_max: 420          # Maximum random interval seconds
  warning_seconds: [60, 30, 10, 5]  # Broadcast countdown warnings before relocation
  no_immediate_repeat: true         # Guarantees the same region will not be selected twice in a row
  player_transition: "LEAVE_IN_PLACE" # LEAVE_IN_PLACE | TELEPORT_TO_NEW
  state_transition: "KEEP_PROGRESS"   # KEEP_PROGRESS | RESET_PROGRESS | DECAY_ONCE
  activation_grace_seconds: 15      # Capture math is paused for 15s to let players reach the new location
  invincibility_seconds: 10         # 10s temporary spawn protection upon warp or shift

  regions:
    - id: "south_fortress"
      display_name: "<gold>South Fortress</gold>"
      weight: 1.0
      min_point: { x: 100, y: 60, z: 100 }
      max_point: { x: 110, y: 75, z: 110 }
      capture_pad:
        min: { x: 102, y: 64, z: 102 }
        max: { x: 108, y: 66, z: 108 }
      warp_location:
        world: "world"
        x: 105.5
        y: 65.0
        z: 105.5
        yaw: 0.0
        pitch: 0.0
```

### Transition & Protection Policies
- **`activation_grace_seconds`**: When an outpost relocates, capture calculations freeze for $N$ seconds while displaying a warmup countdown on the bossbar/actionbar.
- **`invincibility_seconds`**: Grants temporary damage immunity to players who `/outpost tp` or are teleported during a shift. Wears off immediately if the player engages in PvP.

---

## Per-Outpost Language Overrides (`lang:`)

Any outpost config can override global messages from `lang.yml` by defining a `lang:` key:

```yaml
lang:
  telemetry.bossbar.idle: "<gray>[South Outpost] Neutral Zone"
  broadcasts.captured: "<gold><bold>SOUTH CASTLE HAS FALLEN TO %team%!"
```
If an override key is not present, Outposts automatically falls back to the default `lang.yml`.

