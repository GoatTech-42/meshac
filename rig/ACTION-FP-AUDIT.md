# Action check static FP review, candidate8 base89c8625

This branch is an audit plus narrow guards, not a declaration that these checks
are false-positive safe. No native play, Fabric build, rig/config/deploy or
40player execution. Tests prove pure policy/state behavior or document current
counterexamples; they do not prove synthetic fixture inputs are vanilla legal.

## Landed tune, no check removed

- Inventory regular-gap evidence now needs20..159ms intervals and a prior gap
  of at least20ms. Zero/near-zero network batch gaps and clock reversal no longer
  become 'steady beat' heat. Existing rate branch remains.50/100ms repeated
  automation positives retained.20ms is a conservative evidence floor, not a
  human-input bound or a trusted click clock.
- Boatfly still needs5unsupported rises but now5distinct handling ticks on the
  same vehicle id.50duplicate packets in one tick count once; new vehicle id and
  tick rollback reset. Water/ground/bubble support resets remain. Horse path
  unchanged, not an AbstractBoat. Knockback/piston/slime launch lasting5ticks
  remains a native proof gap, not solved by this guard.
- Vehicle/Totem cancellation now respects per-check enforcement and Verdict.LOG.
  Old return true after signal could refuse even a monitor-only signal. Existing
  thresholds/signals/heat unchanged. Shared pure cancel gate tests on/off/LOG.

## Per-check findings still unresolved

Interact: shift-bridge (<.3horizontal per placement) and vertical tower do not
enter horizontal chain, but rate/burst/aim/LOS still apply.10under-feet chain at
>4blocks/s flags regardless of crouch/sprint/speed effects. No source evidence
establishes4as a universal legal maximum. Fast bridging, edge placement into
recently changed blocks, held-right-click and jitter need actual client fixtures.
Slot swap+throw can refuse on first <40ms pair, bow flick3/30s and rapid feeding
switch are not proof of automation. No unsupported broad retune implemented.

Inventory: server click arrival FIFO avoids main-thread batching but not network
coalescing and lacks a cap/packet identity. Container QUICK_CRAFT stages, shift
moves, number-key swaps and double-click pickup are not equivalent to item moves.
10events/100ms may flag legal chest/shulker sorting. Creative handling calls
Config.skips; native creative/container protocol flow still unverified. Hotbar
30/s flags free-scroll input without slot-change identity. No blanket container
or creative exemption added. Need packet click-type/slot/container-id snapshots
and supported-version API compile before a semantic count retune.

Mining:8blocks/s cap increments for every refused-hook block, including instant
break; old comment 'hasted effV near5' is not a verified limit. Haste/efficiency
can change effective per-tick progress without hardness changing to0, so the
hardness-only instant variable doesn't represent all insta-mining. Direction
40deg/600ms, sweep60deg/750ms and short-retarget strikes can fit deliberate human
flicks. digStop early progress uses current tool/block, so tool/effect transition
and veil fake-state mining require tests. No invented higher universal cap or
blanket haste exemption. Need actual vanilla START/STOP/break trace + progress,
state, tool/effects, client/server ticks before setting new bounds.

Totem: preserve Luke-calibrated10/25ms limits and streak weights, do not override
calibration based on model intuition. Network-arrival click vs server pop time
aren't same event clock. Pending click already queued before pop, same-tick
keyboard offhand swap, normal sorting click followed by fill can look reactive.
The current pair classifier doesn't establish intent or physical slot semantics
in every container. Need approved deterministic pop/delayed-click replay and
actual slot mapping. Single fast pair still signals; this branch fixes log-only
refusal, not that semantic ambiguity.

Lag: distinct boat ticks and nonzero inventory cadence are narrow corrections.
They don't authenticate click-generation times. Rate windows and other timing
rules still see network/main-thread batching. No global lag exemption introduced.

## Tests and next gates

ActionEvidenceTest covers lag batch vs5tick positive, vehicle identity/reset,
regular-script positives vs irregular sorting, clock reversal and cancellation.
Eight Python static tests cover each domain, explicitly labelling fast-bridge
and mining-rate counterexamples as remaining risk. Full combined pure suite
must run before report. Native fixtures must show actual successful placements,
sorting, breaks, movement and offhand actions, not merely absence of log lines.
Compile/API and positive hack controls remain separate parent/Box gates.

## Follow-up mining rate and native capture patch

Flat8/s replaced by server-progress-weighted work (MiningRate), bounded20/s for
1tick blocks,10/s for2tick, old8/s for slower/unknown. This applies per block,
not by haste flag/blanket exemption. Positive infinity from zero hardness counts
as1tick; NaN/nonpositive retains old weight. Real hardness formula fixtures and
positive controls in MiningRateTest. Source/API full build still pending.

ActionTrace native logging patch is exact opt-in MESHAC_ACTION_TRACE=1, global
200records/s cap,512char payload bound, dropped count, no verdict consumer. See
rig/RIG-CAPTURE-REQUEST-ACTIONS.md (also shared-round2copy) for fixture requests,
schema and missing evidence. This supersedes flat-cap finding above, not other
mining direction/early-stop/lag risks. No native clearance implied.
