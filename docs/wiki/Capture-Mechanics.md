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

---

## 2. Tug-of-War (`TUG_OF_WAR`)
A symmetrical continuous push-pull tug of war anchored at a neutral midpoint:
```
Team A [0%] ◄────────── 50% [Neutral Midpoint] ──────────► [100%] Team B
```
- Competing factions pull the progress bar toward their respective extremes (0% for Team A, 100% for Team B).
- Pull rate scales dynamically based on the ratio or difference between opposing cappers on the pad.
- If vacated and neutral, the progress bar drifts back to the 50% neutral midpoint.

---

## 3. Ticket Accumulation (`TICKET_ACCUMULATION`)
An objective ticket generation mode:
```
[Holding Team] ────► +X Points/sec ────► Target: 1,000 Tickets (Win)
```
- Uncontested teams generate tickets each tick cycle.
- Progress meter represents ticket accumulation (0% to 100% of target score).
- First team to reach the target ticket capacity captures the point.

---

## 4. Passive Decay (`PASSIVE_DECAY`)
A mode with aggressive decay bleed for abandoned pads:
```
Vacated Pad: 75% ───(bleed rate: 2.5%/s)───► 0% (Auto-Reverts to Neutral)
```
- Prevents abandoned, half-captured points from lingering indefinitely.
- As soon as all players vacate a neutral, partially captured pad, the capture percentage bleeds downward at `passive_decay.rate_per_second` until reset to 0%.

---

## Anti-Jitter & Debounce Window

During intense PvP battles, contested capture bars can bounce rapidly between 99.9% and 100%, causing repetitive capture triggers and duplicate reward execution:
- **Progress Hysteresis Buffer:** When held at 100%, defending ownership remains secure until progress drops strictly below `100.0 - hysteresis_buffer_percent` (e.g. 98.0%).
- **State Debounce Timer:** State transitions (`CONTROLLED` $\leftrightarrow$ `CONTESTED`) require a stabilization window (`state_change_cooldown_seconds: 1.0`) before exiting contested state.
