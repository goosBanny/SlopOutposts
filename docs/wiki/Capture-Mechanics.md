# Capture Mechanics

Outposts features four distinct capture strategies configured via `mechanics.mode`:

---

## 1. Standard Hill (`STANDARD_HILL`)
The classical King-of-the-Hill (KOTH) territory mode:
```
0% ────────► [Neutral Zone] ────────► 100% [Capped]
```
- **Neutral Capture:** When neutral, an uncontested team charges the capture meter from 0% to 100%.
- **Defending Knockdown:** If an outpost is controlled by Team A, invading Team B must knock the progress bar down to 0% before neutralizing the outpost.
- **Defense Recovery:** Team A can step onto their damaged pad to restore their progress back to 100%.
- **Contest Freeze:** If `freeze_when_contested: true`, opposing teams standing on the pad freeze progress in place.
- **Per-Mode Setting:** `mechanics.mode_settings.standard_hill.min_cappers_required` sets the minimum valid cappers required inside the zone to begin capturing.

---

## 2. Tug-of-War (`TUG_OF_WAR`)
A forced 2-team symmetrical push-pull duel anchored at a neutral 50.0% midpoint:
```
Side A (Red) [0%] ◄────────── 50% [Neutral Anchor] ──────────► [100%] Side B (Blue)
```
- **Symmetrical Victory Lines:** Side A wins by pulling the progress bar to `<= 0.0%`. Side B wins by pulling the progress bar to `>= 100.0%`.
- **Contested Pull Dynamics:** If both sides are present, pull speed scales proportionally with surplus player advantage:
  $$\Delta = \text{percent\_per\_second} \times \text{contested\_advantage\_scaling} \times (C_A - C_B)$$
- **Deadzone Buffer (`deadzone_buffer_percent`):** A dead-center neutral buffer around 50.0% (default: `2.5%`, spanning `47.5%` to `52.5%`). While inside this buffer, the outpost is considered deadlocked and neutral.
- **Team Assignment Strategies (`team_assignment`):**
  - `FIRST_TWO_FACTIONS`: Symmetrical faction duel. The first two opposing factions to step on the pad lock into Side A vs Side B. Any 3rd party factions entering the zone are ignored until the engagement resets. Ideal for Factions servers.
  - `AUTO_RED_BLUE`: Automated 2-team partition. Each player entering the zone is automatically assigned to either Red (Side A) or Blue (Side B) to balance team sizes. Ideal for SOLO arenas, Minigames, and casual PvP servers.
- **Personalized HUD BossBar:** Each player receives a personalized perspective of the match (e.g. `<#FF4B4B>RED (YOU)</#FF4B4B> ◄ [45.0%] ► <#4B8CFF>BLUE</#4B8CFF>`), showing which side they are fighting for and which direction the point is moving.
- **Abandonment Drift:** When vacated, the progress bar drifts back toward the configured 50.0% neutral anchor midpoint at `neutral_drift_rate` (default: `2.5%/s`).

---

## 3. Ticket Accumulation (`TICKET_ACCUMULATION`)
An objective ticket generation mode:
```
[Holding Team] ────► +X Points/sec ────► Target: 1,000 Tickets (Win)
```
- Uncontested teams generate tickets each tick cycle.
- Progress meter represents ticket accumulation (0% to 100% of target score).
- First team to reach the target ticket capacity captures the point.
- **Per-Mode Settings:**
  - `target_tickets`: Total tickets required for victory (default: `1000`).
  - `tickets_per_second`: Tickets awarded per second while uncontested (default: `10.0`).

---

## 4. Passive Decay & Abandonment
Every outpost supports background passive decay via `mechanics.behavior.passive_decay.enabled`:
```
Vacated Pad: 75% ───(bleed rate: 2.5%/s)───► 0% (Auto-Reverts to Neutral)
```
- Prevents abandoned, half-captured points from lingering indefinitely.
- As soon as all players vacate a neutral, partially captured pad, the capture percentage bleeds downward at `passive_decay.rate_per_second` until reset.
- In Tug-of-War mode, abandonment drifts toward the 50.0% neutral midpoint.

---

## Anti-Jitter & Control Thresholds

During intense PvP battles, contested capture bars can bounce rapidly, causing jitter and duplicate reward execution:
- **Lose Control Threshold (`lose_control_threshold`):** Defines how low attackers must push the capture bar before the defending team loses control. By default `100.0`, meaning attackers must reduce progress from 100% all the way to 0% to neutralize the point. If set to `60.0`, the point neutralizes as soon as defenders drop to 40% progress.
- **Progress Hysteresis Buffer (`hysteresis_buffer_percent`):** Prevents ownership from toggling on and off when attackers tap the capture pad at full progress. For example, with `hysteresis_buffer_percent: 2.0`, defenders maintain full control until progress drops strictly below `98.0%`.
- **State Debounce Timer (`state_change_cooldown_seconds`):** State transitions (`CONTROLLED` $\leftrightarrow$ `CONTESTED`) require a brief stabilization window (default `1.0s`) before exiting the contested state.
