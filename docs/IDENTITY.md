# meshac identity (not a Vulcan clone)
Vulcan is a quality bar only. meshac has its own vocabulary, UI, commands, config and branding.

## Vocabulary
- A check hit is a **signal** (not a flag/violation). Each player has **heat** (a decaying score), not VL.
- A **mesh** verdict: meshac punishes only when independent signal families agree (movement + timing, combat + rotation...). One noisy check never bans.
- **Trace**: the evidence timeline kept for every signal (positions, rotations, actions). Staff scrub it; not a "replay".
- **Ward**: an exemption (player, world, region, client version). Not "bypass".
- Presets: **calm**, **steady**, **strict** (not "low/medium/high").

## UI
- No chest-GUI. Admin screens use the server-driven Dialog screens built into the game (1.21.6+), with clickable chat panels as the fallback for older versions.
- Screens: Watchlist (players by heat), Player card (heat by family, recent signals, ping, client), Trace viewer, Tune (per check: sensitivity slider, action ladder), Wards.
- Alerts are compact one-line chat cards with hover evidence and click-to-trace, own layout and colors (meshac palette, not red-on-grey).
- Optional later: read-only local web board for traces.

## Commands and permissions
- Root command `/mesh`: `watch`, `card <player>`, `trace <player>`, `tune`, `ward`, `quiet`, `reload`, `selftest`.
- Permission nodes `meshac.watch`, `meshac.card`, `meshac.trace`, `meshac.tune`, `meshac.ward.<family>`.

## Config
- One `meshac.toml` with named presets and per-check `sensitivity` plus an `ladder = ["setback","hold","kick"]` action list. Not Vulcan's per-check enable/VL/command-list layout.

## What is genuinely different
- Fabric-native: runs inside the game, reuses vanilla physics and collision for prediction.
- Mesh verdicts across families instead of per-check VL thresholds.
- Evidence-first: every signal ships with its trace.
- Honest coverage: docs/coverage.md lists misses.
