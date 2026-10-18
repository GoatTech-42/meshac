# Modrinth listing readiness vs the FP/movement work (docs audit only, no listing text changed, nothing published)

Checked against candidate8 (tip 89c8625): docs/MODRINTH-DRAFT.md, README.md, docs/coverage.md, docs/ADMIN.md, docs/FINAL-SWEEP.md, docs/FALSE-POSITIVES.md. There is no CHANGELOG file.

## Blockers before the numbers in the listing can be trusted
1. The legit-play claims ("never touch someone who is just playing", "legit-play suite that has to stay at zero flags", FINAL-SWEEP "57 legit controls, 0 signals") were measured on jar 3729202. Candidate8 changes fly, speed budget, boost windows, blink origin, powder/ medium exemptions and ground tracing. None of it is rig-run or even compiled yet. Rerun the controls on the candidate8 jar before quoting any count.
2. The live ilhsmc log shows real FPs on a shipping-shaped build (116 ground-spoof flags in 14 episodes, mostly Bedrock; microhop, jump 0.63, air jump, fly). The draft's "without flagging legit play" is stronger than the evidence. Suggested softening: "tuned to avoid false flags on normal play; ships with log-only monitor mode so you can check it on your server first" (the /mesh monitor on|off feature is real and already in the draft).

## Text that is now wrong or missing
| File | Line/section | Issue | Needed change |
|---|---|---|---|
| MODRINTH-DRAFT | "Movement: flight, speed, ..." | no mention of the sustained-speed budget | add "sustained speed" |
| MODRINTH-DRAFT | no Bedrock/Geyser text at all | Bedrock players: combat/interact/inventory/mining are skipped, movement runs with looser speed (x1.30) | add a "Bedrock players" limits paragraph (see below) |
| MODRINTH-DRAFT | "Honest limits" | misses list lacks the new ones | add the miss list below |
| README line 35 | "blink (0.75 s and up)" | blink now sets back to the burst origin, and a partial (<0.75 s) is partly covered; FINAL-SWEEP says "blink under 0.4 s" is a miss | state one number after the rig confirms; keep "short blink (about 0.4 s)" under misses until then |
| README line 37 | "legit play hasn't flagged in any control run" | list lacks powder snow, slow falling, honey, water edge, Bedrock, scaffolding | do not extend the list until controls are run for them (docs/SLOWFALL-PROBES.md lists them) |
| README line 41 | "speed at or below sprint-jump pace ... doesn't pretend" | now a 7.4 bps sustained budget exists (limit 4 blocks of overshoot, about 5 s for a 2.0x cheat) | rewrite: sustained speed above about 7.4 bps flags; at or below sprint-jump pace is still not caught |
| coverage.md Speed row (29) | per-packet cap only | add sustained budget, movement_efficiency (Soul Speed) handling, boost windows | one row each |
| coverage.md Fly row (30) | "Anything above it counts" | fly check is skipped in Slow Falling, levitation, water, lava, climbable, cobweb, bubble column, scaffolding, next to honey (+400 ms after), and powder-snow climbs, and falls right after a multi-tick packet | add "does not run in ..." line |
| coverage.md "Not covered yet" (39) | "ice, slime, honey, bubble columns, ... can cause false signals until tested" | honey/bubble/scaffolding/slow falling now exempt but still untested on the rig | keep the sentence; add "exempt by design, untested" |
| coverage.md NoFall row (32) | "Caught in 3 packets" | known Bedrock FP class, partial-block mismatch under study; candidate 0.13 tolerance exists in compare mode only | add a caveat; no change to "3 packets" |
| ADMIN.md line 53 | Bedrock paragraph covers anti-ESP only | add movement-check scope and the known ground-spoof FP; "a name starting with a dot is never trusted" stays true (UUID rule, FpGuards.bedrockUuid) | extend paragraph |
| ADMIN.md | env switches MESHAC_TRACE, MESHAC_GROUND_TOL=compare | undocumented diagnostics | add a short "Diagnostics (off by default)" note in ADMIN, not in the listing |
| FALSE-POSITIVES.md | item 4 "nofall ground spoof ... cause under investigation" | still open | link to docs/GROUND-SPOOF-TOLERANCE.md and BEDROCK-MOVEMENT.md; add rows: powder snow climb 0.2/tick (fly, high jump, microhop), stale-packet fall (fly dy -0.161 vs -0.341), slow falling (fly dy -0.118 vs -0.194, hypothesis until rig run) |
| MODRINTH-DRAFT | "Game versions" | lists 1.21.11, 26.1.x, 26.2, 26.3 | only claim versions that were compiled and tested; candidate8 compiled on none yet, and only 26.2 was compiled for b2e0ed0 |
| repo | no CHANGELOG | Modrinth version entries need one | create CHANGELOG.md for 0.1.0 (first listing) from the points below |

## Proposed limits text (draft wording, for the owner to approve)
- Bedrock players (through Geyser) are not checked for combat, interaction, inventory or mining. Their movement is checked with looser speed limits, and some movement types (gliding, vehicles, climbing, levitation, slow falling, riptide) are not checked at all. A constant 8 blocks per second speed cheat can pass on Bedrock.
- Flight is not checked while a player is swimming, climbing, in a cobweb, on scaffolding or in a bubble column, or under Slow Falling or Levitation.
- Some players can still trigger rare false flags on partial blocks (slabs, snow layers, chests) while on Bedrock. meshac logs these and does not remove anyone for a single flag.

## Proposed 0.1.0 changelog points (only after the rerun in blocker 1)
- Sustained speed check; Soul Speed and boosts handled.
- Blink sets the player back to where the burst began.
- Fewer false flags: powder snow, delayed packets, Slow Falling and similar states.
- Bedrock: movement checks only, with extra tolerance; diagnostics via env switches for admins.

## Not verified here
Whether the owner wants the listing to name Bedrock limits publicly, wording tone, and any claim about 1.21.11 (Java 21 jar) since no 1.21.11 compile of candidate8 exists.
