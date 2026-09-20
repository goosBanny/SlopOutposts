# Multipliers & Economy Perks

Holding an outpost provides passive buffs, combat advantages, and repeated payouts to the controlling faction.

---

## 1. Multipliers Matrix

| Multiplier Key | Default | Description |
| :--- | :--- | :--- |
| `spawner_rate` | `1.5` | Reduces tick delays on mob spawners within faction claims (35% faster spawns). |
| `mob_drop_rate` | `1.75` | Multiplies item drops from hostile mobs killed by faction members. |
| `exp_drop_rate` | `2.0` | Multiplies experience points dropped from mob kills. |
| `damage_rate` | `1.10` | Applies a 10% bonus to player PvP damage dealt by faction members. |
| `shopgui_sell_rate` | `1.35` | Yields a 35% bonus sell price when selling goods via ShopGUIPlus. |

### How Stacking Works
If a faction controls multiple outposts with different multipliers, the engine evaluates the highest bonus across all controlled outposts (`Math.max(...)`) so perks don't compound exponentially into broken server economies.

---

## 2. Comfort Perks
- **`NO_HUNGER_LOSS`:** Faction members holding an outpost with hunger disabled never suffer from hunger depletion while active.

---

## 3. Extensible Action Pipeline (TCA)

Each outpost configuration features a trigger pipeline:
- `on_capture`: Executes immediately when a team reaches 100% and secures the outpost.
- `on_lost`: Executes when the defending team is knocked down and loses control.
- `on_contest`: Executes when enemy forces enter the pad and contest the point.
- `on_tick_reward`: Dispatches repeating payouts at configured intervals.

### Built-in Action Types
- **`BROADCAST`**: Broadcasts MiniMessage formatted text to the server.
  ```yaml
  - type: "BROADCAST"
    message: "<prefix> <gold><team></gold> captured <name>!"
  ```
- **`COMMAND_CONSOLE`**: Executes a command from the server console.
  ```yaml
  - type: "COMMAND_CONSOLE"
    target: "TEAM_ONLINE" # Dispatches command for every online member of the faction
    command: "eco give %player% 2500"
  ```
- **`FACTION_BANK_DEPOSIT`**: Directly deposits currency into the team bank via Vault or ZelTeams Team Bank.
  ```yaml
  - type: "FACTION_BANK_DEPOSIT"
    amount: 1000.0
  ```
- **`SOUND`**: Plays an Adventure sound effect.
  ```yaml
  - type: "SOUND"
    sound: "minecraft:ui.toast.challenge_complete"
    volume: 1.0
    pitch: 1.0
  ```
- **`TITLE`**: Sends an Adventure Title & Subtitle.
  ```yaml
  - type: "TITLE"
    target: "DEFENDING_TEAM"
    title: "<red><bold>OUTPOST UNDER ATTACK</bold></red>"
    subtitle: "<gray>Enemy forces are contesting the South Outpost!</gray>"
  ```
