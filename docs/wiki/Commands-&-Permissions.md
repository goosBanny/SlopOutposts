# Commands & Permissions

## Player Commands

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/outpost help` | `outposts.use` | Displays command list and usage |
| `/outpost list` | `outposts.use` | Lists all registered outposts with live status chips |
| `/outpost info <id>` | `outposts.use` | Displays detailed status, controller, bounds, and capper count |
| `/outpost tp <id>` | `outposts.use` | Teleports to the outpost's configured warp location |
| `/outpost compass <id>` | `outposts.use` | Points your compass directly to the active outpost location |
| `/outpost shop` | `outposts.use` | Accesses the outpost black-market shop |

---

## Admin Commands

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/outpost wand` | `outposts.admin` | Gives the visual boundary setup wand |
| `/outpost create <id> <TEAM\|SOLO> [mode]` | `outposts.admin` | Creates an outpost arena from wand selection with occupancy and capture mode |
| `/outpost delete <id>` | `outposts.admin` | Deletes an outpost and removes its configuration file |
| `/outpost setwarp <id>` | `outposts.admin` | Sets the warp location to your current coordinates |
| `/outpost region add <outpost> <reg_id> <name> [wt]` | `outposts.admin` | Adds a dynamic location region using current wand selection |
| `/outpost region remove <outpost> <reg_id>` | `outposts.admin` | Removes a dynamic region from an outpost |
| `/outpost region list <outpost>` | `outposts.admin` | Lists all configured shifting regions and weights |
| `/outpost region setweight <outpost> <reg_id> <wt>` | `outposts.admin` | Updates the random switch weight of a region |
| `/outpost region shift <outpost> [reg_id]` | `outposts.admin` | Forces an immediate shift to a random or specific region |
| `/outpost reload` | `outposts.admin` | Hot-reloads all configs without interrupting captures |
| `/outpost doctor` | `outposts.admin` | Audits system health, threading, hooks, and bounding boxes |
| `/outpost forcestart <id>` | `outposts.admin` | Force unlocks an arena and initiates a capture cycle |
| `/outpost forcestop <id>` | `outposts.admin` | Force resets an arena back to neutral 0% |

---

## Permission Nodes

- **`outposts.use`**: Granted to default players (true by default).
- **`outposts.admin`**: Full administrative access to manage outposts and run diagnostic audits.
- **`outposts.admin.bypass`**: Allows server administrators to bypass block break/place protections inside outpost zones.
