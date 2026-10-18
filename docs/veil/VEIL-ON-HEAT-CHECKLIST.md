# Veil on vs veil off: heat and flags for mining, movement and combat

Rig server only. Never ilhsmc. Run this after RIG-QUICKSTART sections 0 to 5 passed. Question to answer: does turning the veil on change what the mining, movement or combat checks see? Production Veils stay off until this is done.

## Setup
- Same build for both runs. Only the veil settings change: A = `veilXray` false, `veilEsp` false. B = `veilXray` true, `veilEsp` true, `veilBedrock` false. Optional third run C = B plus `veilBedrock` true with a Bedrock client.
- `/mesh monitor on` so nothing is enforced. Wait for the log line `veil: xray=... esp=...` after each switch and write it down.
- Same arena (flat stone with a buried ore field, a cave pocket, a wall, a bridge gap), same client, same account, same ping. Add 100 ms and 300 ms of ping as separate passes where the row says so.
- Before every pass: `/mesh status <player>` shows heat 0 and no open flags. Reset the player's state if not. Alternate A, B, A, B so a drifting server does not look like a veil effect.
- Do each row once per run, with the same script (hand-driven is fine, same route and same timing each time, or a macro).

## Rows
Mining
- [ ] M1 Dig 10 stone and 10 buried ores (hidden by the veil in B) with a matching pick, to the end, no early release.
- [ ] M2 Dig each kind of fake ore, then let go after 3 ticks on purpose. Compare with 3-tick release on plain stone in A.
- [ ] M3 Tunnel at a buried ore and carry straight on when the neighbour breaks.
- [ ] M4 Dig a fake ore while a second player or TNT removes the block next to it (server resends real stone mid-dig). Note whether the client keeps its progress or restarts.
- [ ] M5 Efficiency V plus haste on stone beside fake ore.
- [ ] M6 Dig ancient debris, a spawner in stone and a nether ore (the veil does not hide these by design, see OPERATOR-GUIDE). Confirm the same result in A and B.
- [ ] M7 Switch `/mesh veil xray false` and true in the middle of a dig.
Movement
- [ ] V1 Walk, sprint, sprint-jump, 60 seconds each along a fixed line.
- [ ] V2 Bridge 30 blocks (sneak bridging), then ladder up and down 20 blocks.
- [ ] V3 Elytra glide and a rocket boost, boat ride, ice slide, soul sand and honey crossing.
- [ ] V4 Teleport, respawn, change dimension twice. Count how many seconds until the other player shows up.
- [ ] V5 Walk past the other player round a corner so ESP hides and shows them (B). Movement heat must not change for either player.
Combat
- [ ] C1 Five PvP rounds, 60 seconds each, same two players, same weapons. Some rounds with the opponent behind a corner and coming out.
- [ ] C2 Hit an opponent who has just come into view from behind a wall (B). The hit must not read as hitting something unseen.
- [ ] C3 Bow and potion throws at a target near the edge of view range.
- [ ] C4 Crystal and TNT in the arena, ten of each.
- [ ] C5 With a second observer account, check nobody sees a frozen or missing model at the end of a round.

## What to write down per row, per run
Heat after the row (`/mesh status <player>`), any flag name and the line text, the `/mesh veil` stats block (stale rebuilds, culls, skipped), and anything odd the player saw. Two numbers side by side per row: A and B.

## Pass and fail
- Pass: no flag in B that is not in A, and heat in B within the spread of two A runs. One flag in a veil run that A never shows is a fail for that row.
- Any row fails: leave the veil off in production, keep the log lines and the timestamps, and send the row number. Do not turn the veil on in production and do not change thresholds to make it pass.
- The veil is expected to add nothing to heat. A difference means a bug in the veil or an interaction we did not know about, and the fix goes into the veil.

## Performance, same session
- [ ] P1 Run PROFILING-PLAN.md cases at 20, 60 and 100 players or bots. Watch the veil line for tick ms and skipped culls. The bench (sim/VeilRayBench) gives 1 to 2 ms per tick for 200 ray tests with plain array lookups and up to about 20 ms with slow lookups, so a value past 10 ms is worth reporting.
- [ ] P2 Server tick time with the veil on and off, 10 minutes each, at the same player count.

## Write down
Client and version, Geyser version, build id, pass letter, row, A against B numbers, screenshots where something looked wrong.
