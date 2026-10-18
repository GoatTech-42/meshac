# Powder/bounce live-trace request, worker-powder-interaction jar

Jar needed: worker-powder-interaction-20261016 tip 9974eba (chain 22aff82 <- 4d68adb <- 5267fec <- 74d3bcb <- 5542f8b <- 3a746b6 <- dc110ad <- 9974eba). Builds from source only; no config change required - the powder and bounce behavior in this jar is on by default, same as production intent.
This is a request to rig owner, NOT permission to change the rig/config/jar or
start clients now. Parent/Box compile scheduling and private meshac-red-only
restriction apply. Reuse existing server/world/clients. No public/ilhsmc traffic,
process killing, new world copies, or extra native clients beyond one Java
operator client (plus the existing authorized cheat client for the two positive
controls). Maintain 2GiB available; one real client at a time per rig budget.

## What this round validates

Model-level sims already green in the jar (43/43 pure tests): PowderInteractionTest,
SneakBounceTest, BounceFlyTest, BounceAirJumpInteractionTest, BounceFixtureTest
(existing slime-10/slime-25/bed-10 fixtures). This round confirms the same
trajectories against a live client, and runs the two cheat-client positive
controls the sims cannot prove (real kick dy/timing from an actual air-jump).

## Bounded capture matrix requested

For each row: short 10..20 s capture, 3 repetitions, exact version/jar SHA,
config and scenario id. Run disabled baseline then candidate jar at the same
conditions. Send cumulative dropped count and any [meshac] SIGNAL lines too.
Prefer no enforcement during instrumentation validation; rig owner selects mode
and records it. No new exemption requested. Legit rows (a-d, f-k) are
zero-signal rows: PASS means no movement SIGNAL lines at the candidate jar that
are also absent at baseline. Cheat rows (e, l) are positive controls: PASS means
at least one air-jump or step SIGNAL within the capture.

World setup (one-time, flat area):
- Powder column: 3x3 powder snow pit, 6 deep, solid floor; adjacent powder
  column with a snow-layer (2 layers, 0.125 step) exit ledge at its top.
- Bounce pads at y=64: slime pad, bed pad, honey pad, each 3x3; drop towers
  giving exact 10-block and 25-block falls onto pad center; slime pad adjacent
  to honey pad (one launch apart) for the cross-landing row.
- Rim/cheat spots: a walkable rim block adjacent to the powder column top, and a
  flat approach beside the column where the body never intersects powder.
- Combo pads: a slime pad edge drop target (straddling slime + ordinary block),
  and a bed placed on top of slime with its own 10-block drop tower.

1. PB-a powder-descend: drop from tower into the powder pit, sink to the floor,
   no input. Expect: steady stuck fall (sim -0.1176/tick), no fly/step SIGNAL.
2. PB-b powder-rest: stand inside powder on a solid block just under the
   surface, body overlapping powder, 10 s still. Expect: dy ~0 packets, no
   fly SIGNAL (resting-in-powder guard), no step SIGNAL.
3. PB-c powder-climb: inside the column, hold jump and climb out the top, ~4
   blocks. Expect: 0.2/tick climb packets, no fly/high-jump/microhop/step
   SIGNAL (powderClimb skip + fold-back path).
4. PB-d powder-exit-snow: climb out of the column onto the snow-layer ledge.
   Expect: 0.125 exit rise, no step-accumulator SIGNAL (exit must not fold the
   skipped climb rise back).
5. PB-e powder-air-jump (cheat client, positive control): body inside powder,
   trigger repeated air-jump kicks. Expect: air-jump and/or step SIGNAL; powder
   cover must not hide the kicks.
6. PB-f slime-trampoline: 10-block fall onto slime, no input, sustain the
   bounce chain 15 s; repeat with the 25-block fall. Expect: no
   air-jump/fly/jump-dy/high-jump/microhop SIGNAL for the whole chain
   (BounceTracker cover; fixtures fp-bounce-slime-10/25).
7. PB-g bed-bounce: 10-block fall onto bed, no input, let the chain decay to
   rest. Expect: clean (0.66 restitution decay; fixture fp-bounce-bed-10).
8. PB-h sneak-mid-chain: bounce on slime, hold sneak just before a re-land.
   Expect: chain stops dead at the sneaked landing; no SIGNAL during or after.
9. PB-i sneak-landing: 10-block fall onto slime while sneaking the whole way.
   Expect: no bounce, no SIGNAL.
10. PB-j honey-landing: 10-block fall onto honey, no input. Expect: no bounce,
    no SIGNAL.
11. PB-k slime-to-honey: bounce once off the slime pad so the next landing hits
    the honey pad. Expect: cover ends at the honey landing; no SIGNAL.
12. PB-l post-disarm-air-jump (cheat client, positive control): right after a
    sneaked slime landing (or a honey landing), trigger repeated air-jump
    kicks. Expect: air-jump SIGNAL; the disarmed tracker must not cover the
    kicks.
13. PB-m powder-rim: stand on the rim block adjacent to the column, jump in
    place 5x. Expect: no SIGNAL (near-powder legit movement).
14. PB-n rim step-cheat (cheat client, negative): step-module climb BESIDE the
    column, body never intersecting powder. Expect: step SIGNAL - the 0.2 climb
    band must not be inherited from proximity alone.
15. PB-o slime-edge: 10-block drop straddling the slime pad edge. Expect: no
    SIGNAL whether vanilla partially bounces or not.
16. PB-p bed-on-slime: 10-block drop onto a bed placed on slime. Expect: no
    SIGNAL; observed bounce must be bed-scale (maxDy ~0.787 per 7e), not
    slime-scale.
17. PB-q powder step-cheat (cheat client, negative): step-module mixed-height
    climb INSIDE powder (dy above and below 0.21). Expect: step SIGNAL - the
    tainted window must count the band packets.
18. GS-legit-strip: walk the partial-block strip (slab/stair/snow/carpet line).
    Expect: no nofall SIGNAL (0.1 strip tolerance per GROUND-SPOOF-TOLERANCE.md).
19. SE-legit-edge: sneak-walk along the strip edges. Expect: no nofall SIGNAL
    (the sneak clamp preserves overlap; settled from source).
20. GS-nofall (cheat client, positive control): NoFall/ground-spoof module
    walking off the strip edge. Expect: nofall ground spoof SIGNAL.
21. BL-burst (cheat client, positive control): Blink module hold-and-release
    while walking. Expect: blink SIGNAL (packet burst).

## Grading contract (reviewer lesson set)

Every capture session is graded by rig/pb_grade.py; the driver verdict in
result.json is provisional. A PASS requires: (1) provenance - scenario, player,
jar SHA, session id, per-leg position deltas, tick health, mod liveness (/mesh
cases answering), setup receipt; (2) a positive witness - at least one
positive-control row (PB-e/l/n/q, GS-nofall, BL-burst) SIGNALing in the same
session, or every zero-signal row in that session is INVALID, not PASS;
(3) state verification - vanilla rows carry the module-OFF pixel confirmation,
and PB-l carries a sneak-pose sample (RCON entity Pose = SNEAKING) proving the
disarm leg really sneaked. Zero signals on a cheat row is INVALID, never PASS.

## Return evidence / stop

Return native-client version, hashes, configuration, timestamps, scenario id,
raw local logs and [meshac] SIGNAL lines per row. State missing/truncated/
dropped evidence. Record FPS/tick health during capture; stop own fixture on
throttle/challenge, RAM<2GiB, unexpected route or user/rep stop, report rather
than killing anything. No zero-signal shortcut to PASS on the cheat rows. Compile
pure/source alone grants no rig release.
