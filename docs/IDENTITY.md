# How meshac should feel

meshac is its own thing. It borrows nothing from other anticheats except the bar for quality.

## Words

Signal, heat, trace, ward. Presets are calm, steady and strict. These names are already in the code and the logs, so they stay.

## Staff tools (planned, not built)

- `/mesh` is the one command. Subcommands: `watch`, `card <player>`, `trace <player>`, `tune`, `ward`, `quiet`, `reload`, `selftest`.
- Screens use the game's own dialog windows, not chest menus. There is a watchlist sorted by heat, a card per player, a trace viewer, and a tuning page. On versions without dialogs, the same information goes out as clickable chat lines.
- Alerts are one line each. Hover shows the evidence, click opens the trace.
- Permissions: `meshac.watch`, `meshac.card`, `meshac.trace`, `meshac.tune`, `meshac.ward`.

## Config (planned)

One file, `meshac.toml`. Pick a preset, then change a check's sensitivity or its action ladder (setback, hold, kick) if you want to. No long per-check lists.

## What is different

meshac punishes when several independent signals agree, not when one check crosses a number. Every signal keeps its trace, so a staff member can look at what happened instead of trusting a score.
