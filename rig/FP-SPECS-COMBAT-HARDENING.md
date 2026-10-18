# Combat hardening from FP replay specs

Base146c9f4, source-only. Read all11JSON specs in
rig/fp-replay-classify.TltmGH/specs. These are scenario specifications, not proof
that any scenario has actually run. Rows1108-1111mark their own origins inferred;
row1106has stale dot-prefix platform instructions that no longer match the code.
No authority, platform identity or clearance is taken from those file claims.

## Tuned live evidence

- **Single-target aim, FP-1105/1107:** keep4miss threshold, but require consecutive
  same-target misses on distinct server ticks. A valid geometric hit resets the
  streak. Changing targets resets it. A batch of100attacks handled in one tick
  contributes one observation, not100. Four genuine separate-tick misses still
  return the same denial reason. Existing other-denial/inside-box outcomes reset
  this evidence so rejected packets don't amplify an unrelated future verdict.
- **Multi-target aim, FP-1105/1107:** keep8hits/4targets/5secondwindow but require
  evidence across at least4distinct handling ticks. This stops one coalesced
  batch alone satisfying the multi-target pattern. Existing geometric positive
  path remains. This is not a full receive-time reconstruction or lag exemption.
- **Reach/LOS/aim rewind:** all historical candidates must be same dimension and
  at most15server ticks old. Old queues after a skipped tick period or dimension
  transition cannot supply stale legal geometry/visibility or false blocking
  evidence. Current legal reach remains included. No blanket reach or aim-box
  expansion and no new player exemption.

## Per-check notes

| Check | Action / remaining legitimate-player concern |
| --- | --- |
| Reach | Current+history retained; add age/dimension eligibility.3.2limit unchanged. Supported interaction-range attributes still need version audit. |
| LOS | Keep current clip plus observed historical visible/blocked samples; ignore stale/cross-dimension samples. Rep's blocked>=2policy retained, no UNKNOWN-veto restored. Dense-budget unknown periods can still miss a fleeting legal sightline, so native corner-strife controls remain mandatory. |
| Aim | Four consecutive same-target distinct-tick misses rather than sparse/batched total misses. Current+vertical/attacker rewind kept. No accurate-player punishment from centre-distance alone. |
| Multi-target aim | Require4handling ticks as well as existing8samples/4targets. Detection not removed, merely separate evidence required. |
| Fast cadence / weak cooldown | Existing evidence-only low-confidence status kept, no promotion or new thresholds. Legal6/8/10CPS and delayed clicks are expected. |
| Snap / same-tick multi-target | Existing low-confidence logging retained with caps. Skilled accurate flicks and lag batches remain allowed. |
| Fake-critical endpoint | Existing low-confidence only. Same-phase real jumps/bounces do not become server-consistency proof. |
| Swap | Existing low-confidence only; legal attribute swapping retained. |
| Features1/7/10 | Still descriptive logs only; no new weights or punishment thresholds. |
| Bedrock | No platform exemption changes. Spec1106dot-name advice is stale; zero combat flags under an existing skip is not a validated classifier. |

## Spec mapping and limits

FP-1105explicitly calls for60second orbit attacks at6/8/10CPS with40ms jitter,
pillars/fences/doorway/ledge,3radii, corner hits and150mslag. FP-1107requires
150/250/400ms jitter/loss plus500/1000/2000msstalls and2minutes melee. Those
native captures remain owed.15tick history covers~750msat20TPS, not a complete
2secondstall. This patch does not pretend otherwise or solve it by widening the
rewind to2seconds (which could create stale-reach advantages).

FP-1101/1102microhop/powder-snow,1103interact look,1104mining, and inferred
1108-1111movement/mining controls are other lanes. Their combat interactions
still need native mixed tests, not a false claim that this source patch clears all.

## Tests

Whole pure suite passes including CombatMissWindowTest: coalesced100input batch,
30alternating strafe misses/legal hits, target-switch isolation, four separate
true misses, timeout and recent/stale/future history.5Python wiring tests confirm
those reducers are called, history filtering in all paths, positive checks remain
and ambiguous logged patterns weren't promoted. No heavy build/native client,
jar/config mutation or actual FP row run. Full API compile and both packet-family
native controls remain required before release.
