# Schedules & Automation 📅

Outposts features an integrated **Cron Scheduler** (`schedules.yml`) that automates when outposts unlock, runs scheduled clan events, broadcasts countdown alerts, and extends matches with **Dynamic Overtime**.

---

## ⚙️ Anatomy of `schedules.yml`

```yaml
config-version: '1'

schedules:
  weekend_war:
    arena: south
    cron: "0 18 * * 6,7"           # Saturday & Sunday at 18:00 UTC
    duration_minutes: 120          # Runs for 2 hours
    overtime:
      enabled: true
      max_overtime_minutes: 15     # Extends match if contested at 0:00
    broadcasts:
      - minutes_before: 15
        message: "<prefix> <name> opens in <yellow>15m</yellow>!"
      - minutes_before: 5
        message: "<prefix> <name> opens in <gold>5m</gold>!"
      - minutes_before: 1
        message: "<prefix> <name> opens in <red><bold>60s</bold></red>!"
```

---

## ⏰ Cron Syntax & Common Presets

Schedules use standard 5-field Unix cron syntax:
```
┌───────────── Minute (0 - 59)
│ ┌─────────── Hour (0 - 23)
│ │ ┌───────── Day of Month (1 - 31)
│ │ │ ┌─────── Month (1 - 12)
│ │ │ │ ┌───── Day of Week (0 - 6 or 1 - 7, where 7 = Sunday)
│ │ │ │ │
* * * * *
```

### Popular Schedule Presets

| Interval | Cron Expression | Description |
| :--- | :--- | :--- |
| **Every Hour** | `0 * * * *` | Fires at the top of every hour (e.g. 1:00, 2:00, 3:00) |
| **Every 2 Hours** | `0 */2 * * *` | Fires every 2 hours on the hour (e.g. 12:00, 14:00, 16:00) |
| **Daily Peak Time** | `0 19 * * *` | Fires daily at 19:00 UTC (prime-time server hours) |
| **Weekend Clan Wars** | `0 18 * * 6,7` | Fires Saturday and Sunday at 18:00 UTC |
| **Friday Night Lights** | `0 21 * * 5` | Fires every Friday at 21:00 UTC |

---

## ⏳ Event Duration & Overtime Engine

1. **Scheduled Activation:** At the configured cron time, Outposts automatically unlocks the arena, broadcasts the opening alert, and begins the capture window.
2. **Countdown Timer:** The event runs for the duration specified by `duration_minutes`. Players can check the remaining time via bossbars or the `%outpost_time_remaining_<id>%` placeholder.
3. **Dynamic Overtime:**
   - If time expires while the outpost is **uncontested**, the event concludes immediately and the current controller retains victory.
   - If time expires while the outpost is **contested** (`is_contested == true`), the match enters **Overtime**!
   - Overtime continues until either:
     - Attackers are wiped and defenders secure the pad.
     - Attackers succeed in capturing the point.
     - The configured `max_overtime_minutes` limit is reached.

---

## 📣 Pre-Event Broadcasts

Announce upcoming battles automatically so factions have time to gather gear and rally players:

```yaml
broadcasts:
  - minutes_before: 30
    message: "<prefix> <name> opens in <yellow>30 minutes</yellow>! Prepare your gear!"
  - minutes_before: 10
    message: "<prefix> <name> opens in <gold>10 minutes</gold>!"
  - minutes_before: 1
    message: "<prefix> <name> opens in <red><bold>60 seconds</bold></red>!"
```

Broadcasts support all standard tokens: `<prefix>`, `<name>`, `<id>`.

---

## 🔄 Strict Scheduling & Reload Safety

- **Strict Start Times:** Scheduled events only trigger when `cron.matches(now)` fires in real time. If the server restarts mid-window (e.g., event was scheduled for 18:00, server restarts at 18:20), the outpost will not retroactively start out of sync.
- **Reload State Preservation:** Running `/outpost reload` re-reads all schedule and language files without interrupting active matches, resetting in-flight overtime, or canceling active timers.

---

## 🧩 Scheduler Placeholders

Track upcoming events easily on Scoreboards, Tablists, and Holograms:

| Placeholder | Example Output | Description |
| :--- | :--- | :--- |
| `%outpost_scheduler_next_<schedule_id>_formatted%` | `23:45:10` / `1d 04:12:00` | Formatted countdown strictly pointing to the upcoming start time. |
| `%outpost_scheduler_next_<schedule_id>_seconds%` | `85510` | Total seconds remaining until next start time. |
| `%outpost_scheduler_is_active_<schedule_id>%` | `true` / `false` | True if this scheduled event is currently running. |
| `%outpost_time_remaining_<id>%` | `28:45` / `Infinite` | Time remaining in the current active event window. |

*(Note: `<schedule_id>` can be the schedule name from `schedules.yml` or the arena's ID).*

---

## 🛠️ Administrative Overrides

- `/outpost forcestart <id> [duration]` — Force-starts an event immediately. `[duration]` is in seconds (default: 1800s / 30m; infinite if `auto_start: true`).
- `/outpost forcestop <id>` — Immediately halts an event and locks or resets the arena.
