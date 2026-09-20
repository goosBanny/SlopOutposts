# PlaceholderAPI Integration

Outposts includes native [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) expansion support under the identifier `%outpost_<...>%`.

---

## Arena-Specific Placeholders

Replace `<id>` with the unique ID of your outpost (e.g. `south`, `desert`, `nether`):

| Placeholder | Example Output | Description |
| :--- | :--- | :--- |
| `%outpost_is_active_<id>%` | `true` | True if the arena is not in post-capture lockout. |
| `%outpost_is_contested_<id>%` | `false` | True if two or more opposing teams contest the zone. |
| `%outpost_is_locked_<id>%` | `false` | True if the arena is currently locked. |
| `%outpost_state_formatted_<id>%` | `Controlled` | Capitalized status chip (`Neutral`, `Capturing`, `Controlled`, `Contested`, `Locked`). |
| `%outpost_progress_bar_<id>%` | `██████████░░░░░` | Formatted segmented progress bar (15 segments). |
| `%outpost_progress_percent_<id>%` | `75.5` | Current capture percentage. |
| `%outpost_controller_team_<id>%` | `Vikings` | Name of the controlling faction, or `None`. |
| `%outpost_capping_team_<id>%` | `Spartans` | Name of the active capturing faction, or `None`. |
| `%outpost_capper_count_<id>%` | `3` | Number of valid players currently inside the zone. |
| `%outpost_time_controlled_<id>%` | `01:45:20` | Formatted duration the controlling team has held the outpost. |
| `%outpost_lock_remaining_<id>%` | `04:15` | Remaining duration on post-capture lockout. |
| `%outpost_<id>_coords%` | `105, 65, 105` | Formatted center coordinates of active region. |
| `%outpost_<id>_x%` / `%y%` / `%z%` | `105` / `65` / `105` | Individual coordinate integers. |
| `%outpost_<id>_current_region_id%` | `south_fortress` | Identifier of active region. |
| `%outpost_<id>_current_region_name%` | `South Fortress` | Display name of active region. |
| `%outpost_<id>_next_shift_seconds%` | `240` | Seconds remaining until the next relocation. |
| `%outpost_<id>_next_shift_formatted%` | `04:00` | Formatted countdown until next relocation. |
| `%outpost_<id>_region_count%` | `3` | Number of configured regions in the pool. |
| `%outpost_<id>_is_shifting%` | `true` | True if dynamic locations are enabled on this arena. |
| `%outpost_<id>_is_warming_up%` | `false` | True if currently in relocation activation grace period. |
| `%outpost_<id>_warmup_remaining%` | `15` | Seconds remaining in relocation activation grace period. |

---

## Global & Schedule Placeholders

| Placeholder | Example Output | Description |
| :--- | :--- | :--- |
| `%outpost_next_event_time%` | `1h 24m` / `Active` / `Overtime` | Schedule countdown until the next active event window. |

---

## Player-Scoped Placeholders

| Placeholder | Example Output | Description |
| :--- | :--- | :--- |
| `%outpost_player_inside%` | `true` / `false` | True if the viewer stands inside any registered outpost cuboid. |
| `%outpost_player_zone%` | `south` / `None` | ID of the outpost arena the player currently occupies. |
| `%outpost_team_outpost_count%` | `2` | Number of outposts the player's faction currently controls. |
