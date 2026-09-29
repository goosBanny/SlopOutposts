# Multipliers & Economy Perks

Holding an outpost provides passive buffs, combat advantages, and repeated payouts to the controlling faction.

---

## 1. Supported Multipliers

| Multiplier Key | Default | Description |
| :--- | :--- | :--- |
| `mob_drop_rate` | `1.75` | Multiplies item drops from hostile mobs killed by controlling members or solo players. |
| `exp_drop_rate` | `2.0` | Multiplies experience points dropped from mob kills. |
| `damage_rate` | `1.10` | Applies a 10% bonus to player PvP damage dealt by controlling members. |
| `shopgui_sell_rate` | `1.35` | Yields a 35% bonus sell price when selling goods via ShopGUIPlus. |

### How Stacking Works
If a faction controls multiple outposts with different multipliers, the engine evaluates the highest bonus across all controlled outposts (`Math.max(...)`) so perks don't compound exponentially into broken server economies.

---

## 2. Comfort Perks
- **`NO_HUNGER_LOSS`:** Faction members holding an outpost with hunger disabled never suffer from hunger depletion while active.

---

## 3. Automated Rewards & Action Pipeline

Each outpost supports an extensive action pipeline capable of awarding cash, crate keys, titles, chat notifications, and executing console commands across multiple triggers:
- `on_capture`: Executes immediately when a team reaches 100% and secures the outpost.
- `on_lost`: Executes when the defending team is knocked down and loses control.
- `on_contest`: Executes when enemy forces enter the pad and contest the point.
- `on_tick_reward`: Dispatches repeating payouts at configured intervals (with support for 5-minute milestone bonuses via `every_seconds: 300`).

👉 **Explore the full guides:**
- **[Actions & Triggers Reference](Actions-&-Triggers)** — Complete target selectors, tokens, and action specifications.
- **[Creating Rewards Cookbook](Creating-Rewards)** — Ready-to-copy configurations for faction bank payouts, milestone bonuses, and tactical alerts.

