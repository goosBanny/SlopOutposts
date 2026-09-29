# Actions & Triggers ⚡

Outposts includes a flexible **Action Pipeline** that executes customizable responses when outpost events occur. You can send MiniMessage chat messages, show action bars, display titles, play sounds, award Vault currency, or execute console commands.

---

## 🎯 Target Selectors

Target selectors define exactly who receives the action:

| Target | Description | Example Use Case |
| :--- | :--- | :--- |
| `CONTROLLER` | All online members of the controlling team (or the winning solo player in SOLO mode). | Payout notifications, holding buffs |
| `PREVIOUS_CONTROLLER` | The team or solo player who just lost control of the outpost. | Loss warning alert on `on_lost` |
| `CAPPER` | The player who initiated or completed the capture/trigger. | Rewarding the capturing MVP with a crate key |
| `CAPPER_TEAM` | All online members of the capturing team. | Team capture rewards |
| `LEADER` | The leader or owner of the controlling faction. | Guild bank or leader bonuses |
| `ZONE` | Every player physically inside the outpost cuboid boundary. | Combat alarms, sound effects |
| `CONTROLLER_ZONE` | Members of the controlling faction currently inside the zone. | Defense reminders |
| `GLOBAL` | Every online player on the server. | Server-wide victory announcements |

*(Aliases: `DEFENDING_TEAM`, `TEAM_ONLINE`, and `PER_PLAYER` can be used for `CONTROLLER`. `ALL` and `SERVER` can be used for `GLOBAL`).*

---

## 🔔 Trigger Events

Actions are grouped under trigger hooks inside `outposts/<id>.yml`:

| Trigger | When It Fires | Available Context |
| :--- | :--- | :--- |
| `on_capture` | When an outpost is successfully captured at 100%. | `<name>`, `<id>`, `<team>`, `<player>` |
| `on_lost` | When the defending team is knocked out of control. | `<name>`, `<id>`, `<previous_team>`, `<team>` |
| `on_contest` | When an enemy steps into the capture zone of a controlled outpost. | `<name>`, `<id>`, `<team>`, `<invader>`, `<invader_team>` |
| `on_tick_reward` | Periodic reward loop (configured by `reward_interval_seconds`). | `<name>`, `<id>`, `<team>`, `<seconds_held>` |

---

## 📦 Action Types

### 1. `MESSAGE` (Chat Messages)
Sends Adventure MiniMessage chat messages directly to target players.

```yaml
- type: MESSAGE
  target: CONTROLLER
  message: "<#36f397>✔ <white>Holding Outpost: <#36f397><b>+$50,000</b>"
```

Multi-line messages are also supported:
```yaml
- type: MESSAGE
  target: PREVIOUS_CONTROLLER
  messages:
    - "<#FC1D53>✘ <white>You were knocked out of <#00AAE4><b><name></b><white>!"
    - "<gray>Your team is no longer earning territory rewards.</gray>"
```

### 2. `ACTION_BAR`
Displays text above the player's hotbar.

```yaml
- type: ACTION_BAR
  target: CONTROLLER
  message: "<gold>★ <yellow>Outpost Buff Active <gray>(+10% Mob Drops)</gray>"
```

### 3. `TITLE`
Displays an on-screen Title & Subtitle.

```yaml
- type: TITLE
  target: CONTROLLER
  title: "<#FC1D53><b>OUTPOST CONTESTED</b></#FC1D53>"
  subtitle: "<white>An enemy has entered the capture zone!</white>"
```

### 4. `SOUND`
Plays an Adventure sound effect to target players or players in the zone.

```yaml
- type: SOUND
  sound: "minecraft:entity.wither.spawn"
  volume: 1.0
  pitch: 1.2
  target: ZONE # Optional, defaults to ZONE
```

### 5. `COMMAND_CONSOLE`
Executes server console commands. Use `%player%` to target individual players or `%team%` for the team name.

```yaml
- type: COMMAND_CONSOLE
  target: CAPPER
  command: "crate give %player% outpost_key 1"
```

### 6. `TEAM_BANK_DEPOSIT`
Directly deposits cash into the faction/team bank (Vault, ZelTeams, or BetterTeams). In SOLO mode, deposits directly into the controlling player's account.

```yaml
- type: TEAM_BANK_DEPOSIT
  amount: 25000.0
```

### 7. `BROADCAST`
Sends a server-wide MiniMessage announcement.

```yaml
- type: BROADCAST
  message: "<prefix> <gold><team></gold> captured <name>!"
```

---

## ⏱️ Secondary Hold Intervals (`every_seconds`)

Any action under `on_tick_reward` can define `every_seconds: <seconds>`. The action will **only** fire when the outpost has been held for multiples of that duration.

```yaml
actions:
  on_tick_reward:
    # Standard payout every 30 seconds
    - type: TEAM_BANK_DEPOSIT
      amount: 5000.0
    # Milestone bonus every 5 minutes (300 seconds)
    - type: MESSAGE
      target: CONTROLLER
      every_seconds: 300
      message: "<#36f397>✔ <white>5-Minute Milestone: <#36f397><b>+$25,000 Bonus!</b>"
    - type: COMMAND_CONSOLE
      target: CONTROLLER
      every_seconds: 300
      command: "eco give %player% 25000"
```

---

## 🏷️ Context Tokens Reference

| Token | Replaced With |
| :--- | :--- |
| `<prefix>` | Plugin prefix configured in `lang.yml` |
| `<name>` | Formatted display name of the outpost |
| `<id>` | Outpost identifier (e.g. `south`) |
| `<team>` / `%team%` | Controlling team name (or player name in solo mode) |
| `<previous_team>` | Name of the team that just lost control (in `on_lost`) |
| `<player>` / `%player%` | Capturing or targeted player's username |
| `<seconds_held>` | Total seconds the outpost has been held |
| `<invader>` | Username of the invader who contested the pad (in `on_contest`) |
| `<invader_team>` | Team name of the invading forces (in `on_contest`) |
