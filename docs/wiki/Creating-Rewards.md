# Creating Rewards 🎁

This guide provides practical, copy-paste reward configurations for your outposts.

---

## 💰 Recipe 1: Economy & Faction Bank Payouts

Deposit currency into the holding faction's bank every 30 seconds and send a clean chat notification:

```yaml
mechanics:
  reward_interval_seconds: 30

actions:
  on_tick_reward:
    - type: TEAM_BANK_DEPOSIT
      amount: 10000.0

    - type: MESSAGE
      target: CONTROLLER
      message: "<#36f397>✔ <white>Outpost Payout: <#36f397><b>+$10,000</b> <gray>deposited to faction bank.</gray>"
```

*(Note: In `SOLO` mode, `TEAM_BANK_DEPOSIT` automatically deposits directly into the solo player's Vault account!)*

---

## ⏱️ Recipe 2: 5-Minute Hold Milestones

Reward teams for holding the point long-term without cluttering chat every 30 seconds:

```yaml
mechanics:
  reward_interval_seconds: 30

actions:
  on_tick_reward:
    # Base reward every 30s
    - type: TEAM_BANK_DEPOSIT
      amount: 2500.0

    # Secondary milestone every 5 minutes (300 seconds)
    - type: MESSAGE
      target: CONTROLLER
      every_seconds: 300
      message: "<#36f397>★ <white>5-Minute Hold Bonus: <#36f397><b>+$50,000</b> <gray>(Held: <seconds_held>s)</gray>"

    - type: COMMAND_CONSOLE
      target: CONTROLLER
      every_seconds: 300
      command: "eco give %player% 50000"

    - type: SOUND
      target: CONTROLLER
      every_seconds: 300
      sound: "minecraft:ui.toast.challenge_complete"
      volume: 1.0
      pitch: 1.5
```

---

## 🔑 Recipe 3: Crate Keys for the Capturing Hero

Award a physical crate key to the specific player who capped the point (`CAPPER`), while granting faction bank money to the whole team:

```yaml
actions:
  on_capture:
    - type: BROADCAST
      message: "<prefix> <#00AAE4><b><team></b></#00AAE4> has secured control of <name>!"

    # Give key to the MVP who capped
    - type: COMMAND_CONSOLE
      target: CAPPER
      command: "crate give %player% outpost_key 1"

    # Notify the capper directly
    - type: MESSAGE
      target: CAPPER
      message: "<#00AAE4>★ <white>You received an <gold><b>Outpost Crate Key</b></gold> for capturing the point!"

    # Deposit team bonus
    - type: TEAM_BANK_DEPOSIT
      amount: 100000.0
```

---

## 🚨 Recipe 4: Tactical Defense Alerts

Alert defending faction members instantly with on-screen titles and sounds when enemies contest their outpost, and notify them if they get knocked out:

```yaml
actions:
  # Alert defenders when enemies step on the pad
  on_contest:
    - type: TITLE
      target: CONTROLLER
      title: "<#FC1D53><b>OUTPOST CONTESTED</b></#FC1D53>"
      subtitle: "<white>Enemy forces entered the capture zone!</white>"

    - type: SOUND
      target: CONTROLLER
      sound: "minecraft:block.bell.use"
      volume: 1.0
      pitch: 0.8

    - type: MESSAGE
      target: CONTROLLER
      message: "<#FC1D53>⚠ <white><name> is under attack by <red><b><invader_team></b></red>! Defend the point!"

  # Notify defenders if they lose ownership
  on_lost:
    - type: MESSAGE
      target: PREVIOUS_CONTROLLER
      message: "<#FC1D53>✘ <white>You were knocked out of <#00AAE4><b><name></b><white> and stopped earning rewards!"

    - type: SOUND
      target: PREVIOUS_CONTROLLER
      sound: "minecraft:entity.wither.hurt"
      volume: 1.0
      pitch: 0.8
```

---

## ⚔️ Recipe 5: PvP Combat Advantage & Mob Drops

Combine actions with passive outpost multipliers inside the arena config:

```yaml
multipliers:
  mob_drop_rate: 1.75    # 75% more mob drops for holding team
  exp_drop_rate: 2.0     # Double XP from mobs
  damage_rate: 1.10      # 10% bonus PvP damage deal
  shopgui_sell_rate: 1.35 # 35% higher sell prices in /shop

actions:
  on_capture:
    - type: ACTION_BAR
      target: CONTROLLER
      message: "<#36f397>✔ <white>Outpost Perks Activated: <#00AAE4>+10% PvP Damage & +75% Mob Drops"
```
