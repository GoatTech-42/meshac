# Candidate6 combat patch

Base1f412f5. Source-only review candidate; not a tested release or rig deployment.

## Correctness changes

- Preserve current reach candidate while including coherent tick history. A
  player who moved into range since the last tick sample no longer loses that
  current legal pair when history is present.
- Aim tests current geometry plus stored eye/target box pairs, including vertical
  displacement and attacker movement. Accepted historical/edge hits no longer
  count as multi-target aim misses merely for being far from a box centre.
- Store LOS visibility when the eye/target tick sample is collected. Rewound
  hits no longer test historical positions against only current blocks, which
  could falsely reject a hit after a door closed or a block was placed.
- End a snap streak on every nonqualifying attack, not only after a600ms gap.
- Clear MULTI on disconnect with the other combat state.

## Low-confidence evidence, not punishment

Cadence/fast-click, weak cooldown, precise snapping, same-tick multi-target,
short-fall endpoint and swap timing patterns remain counted and logged as
`[combat-evidence] confidence=low punishEligible=false`. They no longer set
Combat.check's return reason or call Verdict. There is no heat, attack cancel,
kick or ban from these ambiguous patterns. Full trace telemetry is preserved.
This is the explicit parent decision to preserve flagged evidence while keeping
unproven legal-input ambiguities out of punishment, not a newly claimed detector.

`flatSinceLast` stays only as a legacy evidence label. Equal Y at attack endpoints
is not called server-motion consistency and is not used to reject an attack.
Repeated genuine jumps at the same phase must not be punished as fake criticals.
A genuine consistency classifier remains needed before promoting that evidence.

Geometric reach, LOS and repeated true aim misses still return attack-denial
reasons. Those checks also need native controls; preserving them is not FP proof.
The multi-target geometric rule now uses actual accepted/missed rays rather than
centre offset, but still needs the rig replay. No A15 separation is claimed.

## Tests

`CombatEvidenceTest` runs8assertions through reducers used in the live code:
current+historical reach, legal current/rewound aim and interrupted snap streaks.
`python3 rig/test-candidate6-combat.py` has8wiring guards verifying those reducers,
LOS snapshot use, full disconnect cleanup and that low-confidence paths cannot
return a denial reason or invoke Verdict. Existing candidate5 predicate simulations
are historical shape tests, not validation of the new outcome policy.

No full Fabric compile, native client replay, screenshots or performance
measurement was run by this source-only assignment.

## Risks and remaining work

The LOS snapshot adds up to4block clips per nearby living entity per player per
tick (within12blocks). That extra work is bounded spatially but can be expensive
in dense farms. The representative must benchmark or replace it with a smaller
on-demand visibility cache before claiming the light-footprint gate is met.
Snapshots are server-tick observations, not exact client view timestamps. A block
change between samples and an attack is still not fully reconstructed; no exact
network/clock claim is made.

Reach still uses a fixed3.2limit and15tick history. Validate supported range
attributes and version-specific semantics. No new platform exemptions were added.
Historical samples across teleports/dimensions remain a separate lifecycle review.
10-11 crown strafing, bad-ping batching, microhop/bounce, vertical tracking, effects,
vehicles, legal swapping and both packet families still require real negative
controls on the existing private rig. Never infer release clearance from unit PASS.
