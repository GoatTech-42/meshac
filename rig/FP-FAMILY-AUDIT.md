# FP-family source follow-up / candidate9e

Base9515209 (combat-lag tip), linear per-fix branch worker-fp-family-audit-20261016.
Shared fp-repros contains7memory-derived drafts, not10-11verified native replays.
No live ilhsmc access, native execution, build/deploy, rig change or new worlds.

09b3257: Mining.look never had a caller. Feed yaw/pitch from all rotation-bearing
Movement packets alongside Interact.look, including rotation-only packets before
position early return. Existing10clienttickrecentAim now actually refreshes on
aiming during a dig. Age check rejects future ticks rather than negative age
passing <=10. Pure test validates refresh/expiry/reset; wiring test requires live
call. Native sculk/hoe/efficiency sweep proof remains, especially pre-handler eye
vs packet movement position and unknown client-tick sources. No new TTL/reach.

59fd306: historical Interact rotations were all cast from CURRENT eye, so walking
while opening an edge-angle door/button could lose its actual legal ray. Keep
coherent eye+rotation+dimension per existing6slot/750msring. Same8range/0.3box
padding. Pure pose math/age/dimension/behind-control tests, live wiring assertions.
Does NOT replay historical block geometry: current GhostHand outline clip and
AirPlace state can still reject stale client world. Doors changing state require
native block snapshots, not a claimed all-FP fix. Pose allocation is bounded6live
entries/player but new per-rotation garbage;40player allocation cost unprofiled.
Old yaw/pitch ring retained for existing bookkeeping, could later simplify after
API/native validation. No broad 'all interact behind player' bypass.

29a7e3e: Movement accepted0.42..0.66ground rise as vanilla auto-step in accumulator
but independent dy>0.62direct step and previous-ground high-jump could flag the
same rise. One StepEvidence helper shares EXISTING0.66(+existing boost allowance)
ceiling. Ground accepted steps no longer trip high-jump; airborne rise still
checked, >.66direct and multi-rise accumulator>.65retained. No guessed new
ceiling, no stair-type whitelist. Old boost allowance itself isn't a measured
vanilla envelope and remains native proof gap. Actual customSTEP_HEIGHT/lag
merged movement support still unverified. Slow split-step positive retained.

54Java pure classes PASS. All14non-proxyexistingPythonentrypoints PASS; new3test
family wiring entrypoint PASS. Proxy loopback test first-runFAIL release250ms
metric again. Existing timing flake remains open, do not label all-Python stable.
No code changes made to hide proxy failure. Native legal-action success within
10%baseline and paired cheat positives needed, including high-ping all families.
