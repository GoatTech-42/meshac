# Running meshac: staff commands and veil settings

Everything here is for server staff. Players never see any of it.

## Who can use it

`/mesh` needs operator level 3 (the vanilla "admin" level). The console can use it too. Nothing else is checked, so only give that level to people you would trust with bans.

## Commands

| Command | What it does |
| --- | --- |
| `/mesh cases [player]` | Last 10 cases, or the cases for one player. |
| `/mesh case <id>` | One case in full: action, player, reason and the evidence list. |
| `/mesh ban <player> <duration> <reason>` | Ban. Duration `0` is permanent. Otherwise `30s`, `10m`, `3hr`, `1d`, `2w`. A plain number means minutes. Works on offline players if they have joined before. |
| `/mesh kick <player> <reason>` | Kick with a case on record. Not a ban. |
| `/mesh pardon <id>` | Marks a case pardoned so it stops counting as an offence. Only cases meshac made itself count toward the ladder, so this matters for automatic cases. |
| `/mesh unban <player>` | Lifts active bans for that player. The offences stay on record, so the next one still escalates. |
| `/mesh status <player>` | Heat, offence count and the next removal step for an online player. |
| `/mesh watch` | Toggle live flag messages for you. |
| `/mesh monitor [on\|off]` | Monitor mode: detector flags are logged; no detector kicks, bans, setbacks, damage or hit denial. Legacy `egregiousChecks` cannot bypass it. |
| `/mesh check [name] [on\|off]` | List disabled checks, or switch one check off or on. |
| `/mesh veil` | Show the veil state. |
| `/mesh veil xray <true\|false>` | Anti-xray on or off. |
| `/mesh veil esp <true\|false>` | Anti-ESP on or off. |
| `/mesh veil off` | Both off. |
| `/mesh reload` | Re-read the config and the case file. |

## What sticks and what does not

`monitor`, `check` and `veil` change the running server only. A restart or `/mesh reload` goes back to what `config/meshac.json` says. If you want a change to last, edit the file: `monitorOnly`, `disabledChecks`, `veilXray`, `veilEsp`.

Turning a check off with `/mesh check <name> off` stops it for everyone: no flags, no heat, no setbacks. Use it to stop a false flag while you look into it, then turn it back on. Names that do not exist are refused, and a typo in the config is logged as a warning at start.

## The audit line

Every command that changes what meshac enforces writes one line to the server log, whatever the command feedback gamerule says:

```
[meshac-admin] actor=<who> action=<what> detail=<details>
```

Actions are `veil.xray`, `veil.esp`, `veil.off`, `monitor`, `check.<name>`, `reload`, `pardon`, `unban`, `ban`, `tempban`, `kick`. Bans and kicks also create a case, so the case id is in the line. Text typed by staff is cleaned before it is logged: no line breaks, no `=`, at most 120 characters per field. To see who changed what, grep the log for `[meshac-admin]`.

## Veil settings

The veil is two separate features. Both are off until you turn them on.

**Anti-xray** (`veilXray`) hides ores, chests and spawners that are fully buried, and puts a small share of fake ore into buried stone. The rule is fail open: if a chunk next to a block is not loaded, the block is shown. Players see the change in chunks they load after the switch.

**Anti-ESP** (`veilEsp`) stops sending players and mobs a client has no line of sight to. It tests from the real eye position, so a freecam camera does not help. Leaves, glass, fences, doors and similar never block the test. An entity within 8 blocks, or one the player hit in the last 3 seconds, is never hidden. Spectators and creative players are exempt.

**Bedrock players (Geyser).** Bedrock players are recognised by a UUID with a zero high half, which is what Geyser and Floodgate hand out. A name starting with a dot is never trusted. Anti-ESP never hides anything from a Bedrock player unless you set `veilBedrock` to true, because that path has not been proven with a Bedrock client yet. Bedrock players can still be hidden from Java players. Anti-xray cannot be switched per player: chunks are built once and shared, so Bedrock players get the same fake ore and hidden chests. Geyser turns those into ordinary block updates, so it should look the same. If a Bedrock player reports a missing chest or odd ore, `/mesh veil xray false` and tell us what they saw. This is checked against the source only, not with a Bedrock client.

Bedrock movement: combat, interaction, inventory and mining checks skip Bedrock players. Movement checks still run, with 30% more speed room, and they stay off while a Bedrock player is gliding, riding, climbing, levitating, slow falling or riptiding. Expect more ground-claim ("nofall ground spoof") flags from Bedrock players than from Java players, mostly on partial blocks like slabs and snow layers. A single flag removes no one, and nothing here has been tried with a real Bedrock client yet.

**Example settings** in `config/meshac.json`:

```json
{ "veilXray": true, "veilEsp": false }
```
Anti-xray only. The safest first step.

```json
{ "veilXray": true, "veilEsp": true }
```
Both on, Java players only get culled.

```json
{ "veilXray": true, "veilEsp": true, "veilBedrock": true }
```
Also cull for Bedrock viewers. Only after a Bedrock player has tested it.

```json
{ "veilXray": false, "veilEsp": false }
```
Everything off. This is the default.

If something looks wrong on a live server, use `/mesh veil off`. Hidden entities come back within a tick. Chunks a player already received keep their fake ore until that player reloads the chunk (relog or walk away and back). Turn it on again once you have the cause.

`/mesh veil` prints the number of stale plan rebuilds and, while anti-ESP is on, a second line with the hidden pairs, hides per tick over the last 10 seconds, the time the check takes per tick (average and worst), rays used, how many new culls were skipped because the per tick budget ran out, and whether the entity index is in use. Counters start at zero on every start, reload and `/mesh veil` change. Nothing is saved. At start and reload anti-ESP runs a small self-check and logs "anti-ESP self-check passed"; if it fails it turns itself off and logs why. "tracked index OFF" means the faster entity lookup failed and the old full scan is running: nothing is wrong for players, send us the log line. A count that climbs steadily means some block change is not reaching the veil. Send us the count and what players were doing.

## Veil troubleshooting

Run `/mesh veil` and read the second line (it shows while anti-ESP is on). Counters start at zero on each start, reload and `/mesh veil esp on`.

| What you see | What it means | What to do |
|---|---|---|
| `tracked index OFF, full scan in use` | The fast entity lookup failed once and switched itself off. Players are not affected, the check just costs more. | Find the "tracked-set index disabled" line in the log and send it to us. `/mesh reload` tries the fast path again. |
| `N fallback scans` climbing | The fast path is on but some ticks used the slow one. | Same as above. A few is fine. |
| `new culls skipped for budget` climbing fast | More pairs need a line of sight check than the per tick budget (200) allows. Nothing is hidden wrongly. New hides just wait. | Normal on a crowded server. If hidden pairs stay at 0 and this climbs, tell us the player count. |
| `esp tick ... worst` over about 5000 us | A slow tick. The first minutes after a start run slower while Java warms up. | Look again after 10 minutes. If it stays high, send the line and the player count. |
| `hidden pairs` high while few players are online | Pairs from players who left may linger for up to a minute. | Wait a minute. If it keeps growing, send us the line. |
| `hides/tick` stays high with players standing still | Something flips between hidden and shown. | Turn anti-ESP off with `/mesh veil esp false` and send us the line and where it happens. |
| `/mesh veil esp true` answers "stays off, the self-check failed" | The built-in test of the hiding rules failed. This is a bug, not your config. | Send the log line. Leave it off. |
| A player says someone vanished or was missing | Hidden entities come back within a tick when you turn anti-ESP off. | `/mesh veil esp false`, then ask what they were doing and who. Bedrock players are never hidden from unless `veilBedrock` is true. |
| The warning "config key ... is ignored" | A veil key in `meshac.json` has the wrong spelling and is not read. | Fix the spelling to what the warning says, then `/mesh reload`. |

If you are unsure about any of it, `/mesh veil off` is safe at any time.

## Piston water fix

`fixPistonWater` is off by default. When on, it stops the short-pulse piston trick that keeps waterlogging blocks, which is what flood-war machines depend on. Normal machines are not touched and no player is punished for it.

## If a player says they were flagged unfairly

1. `/mesh cases <player>` and `/mesh case <id>` to see what tripped.
2. If it was a real mistake, `/mesh pardon <id>`.
3. If the same check keeps firing on normal play, `/mesh check <name> off` for now, and send us the case id.

## Diagnostics (off by default)

Two environment switches for the server process, meant for testing on a rig, not for normal servers:

- `MESHAC_TRACE=1` prints one trace line per move check, including a `groundctx` line with the blocks around a player whose ground claim has air below it.
- `MESHAC_GROUND_TOL=compare` logs a `[groundtol] would-have-passed` line when a wider ground tolerance would have accepted a claim. It only logs and never changes what gets flagged.
