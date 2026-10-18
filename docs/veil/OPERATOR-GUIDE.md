# Veil operator guide

The veil is meshac's answer to x-ray and ESP cheats. It works by not sending a client things it has no business knowing. It is off until you switch it on, and you can switch it off at any moment.

## What it does
- **Anti-xray** (`veilXray`). Ores, chests and spawners that are completely buried are sent as plain stone, with exceptions: ancient debris, ender chests, spawners under stone and nether ores are sent as they are (see the list below). A small share of buried stone is sent as fake ore. Once a block next to a buried ore is removed, the real ore is sent right away.
- **Anti-ESP** (`veilEsp`). Players and mobs that a client cannot see from where it stands are not sent to it. They are sent again within a fraction of a second once they come into view. Nothing within 8 blocks is ever hidden, and nothing the player hit in the last 3 seconds. Spectators and creative players see everything. For a few seconds after joining, respawning or changing dimension, nothing is hidden.
- **Bedrock** (`veilBedrock`). Bedrock players joining through Geyser are not hidden from by anti-ESP unless you turn this on. Do that only after a Bedrock player has tested it.

## Turning it on
Edit `config/meshac.json`, then `/mesh reload`:

```json
{ "veilXray": true, "veilEsp": false }
```

Start with anti-xray alone. Add `"veilEsp": true` when you are happy. The log says which parts are on after every start and reload, for example `veil: xray=on esp=off bedrockViewersCulled=no`. If a key is spelled wrong the log says so and the veil stays off.

You can also switch parts for the running server only, without touching the file:

| Command | Effect |
|---|---|
| `/mesh veil` | State, plus a counters line while anti-ESP is on. |
| `/mesh veil xray true` or `false` | Anti-xray on or off. |
| `/mesh veil esp true` or `false` | Anti-ESP on or off. Turning it on runs a short self-test first and refuses if it fails. |
| `/mesh veil off` | Both off. |

A restart or `/mesh reload` goes back to what the file says. Every one of these commands writes a line starting with `[meshac-admin]` to the server log, with who ran it and what changed.

## Turning it off
- Fastest: `/mesh veil off`. Hidden players and mobs are back within a tick.
- For good: set both to `false` in the file and `/mesh reload`.
- Chunks a player already received keep their fake ore until that player loads them again. Have them relog, or walk away and back, if you need it gone at once.

## What players may notice
Nothing, when it works. These are the things to ask about if someone complains:
- A player or mob that pops in a moment late when coming round a corner. The check runs every fifth of a second, so it should not be more than that.
- A fake ore in stone that gives stone when mined. That is on purpose. Fakes are removed as soon as a tunnel opens next to them.
- A chest that looks like stone from a distance when buried. It shows when the block next to it is dug out.
- On Bedrock through Geyser: the fake ore and hidden chests apply (chunks are shared) but nothing is hidden by anti-ESP unless `veilBedrock` is true. Bedrock has not been tested with the veil yet. If a Bedrock player reports a missing chest or an ore that breaks as something else, turn anti-xray off and tell us what they saw.

## The risks, plainly
- A wrong hide is a vanished player. The code leans the other way on every doubt: unloaded chunk, unknown block, error, all of them show the real thing. Still, it is new, and the first runs on a live server should have you watching.
- The veil does not directly add violation heat or issue bans or kicks. It changes chunk packets and entity tracking. Bugs or added load could still interfere with normal play or with other checks, so mining, movement and combat must be tested with the veil on before you enable enforcement. One known place to watch is the mining check: see section 6 of `docs/veil/RIG-QUICKSTART.md`.
- Anti-ESP costs server time. The counters line shows how much. Under heavy load it checks fewer new pairs per tick, which means a hidden-worthy player is seen a little longer, never that a visible one is hidden.
- Anti-xray fake ore changes the ore count a cheater sees, but a determined cheater can still learn from mined blocks. It stops casual x-ray, not everything.

## What is not hidden, and why
A client times its dig by the block it is shown, and the mining check judges by the real block. If the real block is more than twice as hard as the one shown, a normal dig could read as an early stop. So the veil leaves those blocks alone: ancient debris, ender chests, spawners in ordinary stone, and ores in the nether (quartz, nether gold). Spawners under deepslate, chests and ordinary ores are still hidden. Hardness numbers are from the game jars for 1.21.11, 26.1.2, 26.2 and 26.3. The cost is that someone with an x-ray can see ancient debris. If the rig shows the mining check stays quiet with those blocks hidden, the rule can be loosened.

## Reading `/mesh veil`
First line: stale plan rebuilds, neighbour and border rebuilds. Small numbers are normal. A neighbour count climbs while chunks load around players, and settles. A stale count that keeps climbing while nobody builds means some block change is not reaching the veil. Send us the numbers and what players were doing.

Second line (anti-ESP only): hidden pairs, hides per tick over 10 seconds, time per tick, rays used, culls skipped for budget, and whether the fast entity lookup is on. The troubleshooting table in `docs/ADMIN.md` says what each means.

## If something is wrong
1. `/mesh veil off`.
2. Note what the player saw, where, and the time.
3. Copy the `/mesh veil` lines and anything from the log that starts with `[meshac]` about the veil.
4. Send them to us. Turn it on again after we have the cause.

## Before you trust it
`docs/veil/RIG-QUICKSTART.md` is the checklist we run with one Java and one Bedrock client. It includes a cross-check of the mining check against the fake ore timing. Nothing in the veil has been run on a live server yet, and enforcement should stay in monitor mode (`/mesh monitor on`) for the first runs with the veil on.

## Bedrock names in the exempt lists
`exemptPlayers` and `exemptChecks` match the name the server sees. An unlinked Bedrock player gets Floodgate's prefix in front of the name (a dot by default, spaces turned into underscores), so "My Name" is ".My_Name". A linked Bedrock player joins under the name of their Java account, with no prefix. Check `username-prefix` in Floodgate's config and write the name the way the player list shows it. The Bedrock skip itself never uses names: it follows the UUID and Floodgate's own answer.
## Feature rules are not governed by monitor mode
`monitorOnly` controls anti-cheat punishment. It does not disable veil concealment or the dragon-egg ender-chest rule.
- `veilEnabled`: master veil switch (default true). `false` disables both xray and ESP; existing `veilXray` and `veilEsp` still default false and must separately be enabled. This repository uses flat JSON keys, not a nested `veil.enabled` object.
- `enderchestEggRule`: default true. `false` allows a dragon egg, including one in a shulker box or bundle, into an ender chest.
Set the chosen keys in `config/meshac.json`, then `/mesh reload`. Neither key authorizes bans, kicks, setbacks or hit denial. The combat hook permits hits when Verdict returns LOG; monitor mode never enforces detector signals, including legacy `egregiousChecks` entries.
