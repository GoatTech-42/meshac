# Veil hardening branch, for Luke

Branch `veil-harden-20261016`, seven commits on top of candidate5 (1f412f5), plus the one from this round. Nothing is pushed. None of the game code in it has been compiled: the Box does that. The pure parts have tests that run with plain `javac` (`sh sim/run-tests.sh`, all pass on the box).

Production veils stay off. Every new switch defaults to off or to the safe side.

## Commits

| sha | what |
|---|---|
| 667f95b | Edge-case tests for xray, ESP and freecam geometry. A log line for every staff command (`[meshac-admin]`). |
| dcefd8c | The same log line for ban and kick. `docs/ADMIN.md` for staff commands and veil settings. |
| ace6258 | Faster ESP: no allocation per check, a flat table for hidden pairs, cleanup of players and entities that are gone. About 65 ms to 12 ms per full cycle on a loaded box (rough). |
| d99b4ed | ESP looks only at entities near each player (a grid built from the tracked set) instead of every loaded entity. Falls back to the old scan on any error. |
| 617b968 | Bedrock viewers are never culled by anti-ESP unless `veilBedrock` is true. Config examples and a Geyser note in the docs. |
| d172466 | A misspelled veil key in the config now gives a warning. One log line shows the active veil state. The Java plus Bedrock test checklist `docs/veil/RIG-QUICKSTART.md`. |
| e694b20 | A start-up self-check, and status counters on a second line of `/mesh veil`. |
| this round | `/mesh veil esp true` reruns the self-check and refuses to switch on if it fails. Troubleshooting table in the docs. |

## What the tests prove

Pure Java, no Minecraft: the hiding rules (three failed checks hide, near and exempt never hide, one pass shows again), the grid never loses an entity within range, Bedrock detection cannot be faked with a name, typo detection, counters, the self-check, the admin log format, and a speed ceiling for 100 players with 500 entities.

## What only the rig can tell us

- Everything marked uncompiled still has to build on 1.21.11, 26.1, 26.2 and 26.3. I checked the tracked-set field names with javap on 26.2 only.
- Whether the grid saves real time at real entity counts, and how many loaded entities are untracked.
- The tick cost and the 200 ray per tick budget under 40 or more players.
- A Bedrock client through Geyser: fake ore and hidden chests in chunks (shared, so Bedrock gets them too), and, with `veilBedrock` on, entities that come back after a wall.
- Whether `/mesh veil off` really brings everyone back on both clients.
- Dense mob farms near many players. The grid does not help there.

## Known gaps

- Anti-xray cannot skip Bedrock players, chunks are built once and shared.
- The self-check tests the rules, not the Minecraft wiring. Only the rig covers the wiring.
- Runtime `/mesh veil xray true` does not rerun anything. It only matters for new chunks.
- The room veil (viewer-relative concealment) and the candidate4 audit are on their own branches: `worker-b3-viewer-20261016` and `audit-cand4-20261016`.

## First steps on the rig

1. Build the branch. Fix compile errors first, they will be small and mechanical.
2. Follow `docs/veil/RIG-QUICKSTART.md` top to bottom with one Java and one Bedrock client.
3. Watch the second line of `/mesh veil` while 20 or more bots join. Send us anything odd.
