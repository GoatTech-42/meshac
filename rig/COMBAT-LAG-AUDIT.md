# Combat lag/hitbox review on candidate9c

Isolated branch worker-combat-lag-audit-20261016 base241df23. No native test,
heavy compile, check-wide lag exemption, wider reach/rewind or deploy.

## Fix1: accepted ray vs historical miss median (593859b)

Multi-target logic already records accepted edge/rewound rays as0miss. However,
when enough old miss9values dominate the median, a CURRENT legal accepted hit
could still return 'hits on targets without aiming'. Require current aimAccepted
false along with unchanged8hits/4targets/4ticks+median>0.9. Same-target accepted
reset and true multi-target missed-ray positives remain. Test scenarios label
ping/knockback/riding/teleport/large-hitbox coherent accepted inputs; they test
policy algebra, not actual geometry/rendering of these native scenarios.

## Fix2: subject/lifecycle isolation (e231616)

History is keyed by numeric id but now samples carry stable UUID and every
reach/LOS/aim consumer checks UUID+dimension+tick age. Recycled ids can't borrow
old blocked LOS/box (or false legal visibility). Aim misses similarly reset on
UUID change despite same numeric id. New dimension replaces player's evidence
state (aim, multi-target, features/log budget/LOS cache), not only sample filter.
Disconnect retains existing map/recorder cleanup. Identity helper+miss accumulator
pure tests prove isolated old3/new1vs true4distincttick misses. No requirement to
match current position/velocity, so ordinary knockback/teleport history still
provides legal bounded candidates, not false fast-motion exclusions.

## Diagnostic vs live findings

No combat shadow budget drives Verdict. Features1/7/10andlowConfidence remain
nonpunitive. Diagnostic reach/aim err describe current geometry, whereas live
checks may accept historical coherent eye+box: raw err9orcurrentreach>3.2alone
is not proof the attack should have been denied. [a15] already includes both
reach andrewindReach; recorder reach/err are current, no trusted click time.
Do not use raw recorder summary as a live verdict replay without historical
inputs. Trace history is conditional Trace.ONandbounded; it is not classifier
state. Entity id/UUID scope strengthens live source but old recorder schema still
contains numeric id only, so offline joins need session/UUID context from fixture.

## Open native tests / unresolved risks

- Current+15tick history cannot establish genuine click time for2secondstalls.
  Do not widen rewind/reach or treat client tick/ACK claims as trusted clock.
- LOS sparse8observation budget, observed blocked>=2andUNKNOWNnot veto remains
  parent policy. Brief legal visibility between samples may still be missed.
  Test door/pillar/slab/fence/ledge orbit, ping spikes and corners at attack time.
- Riding eye/targetbox height changes, entity teleport between ticks, knockback
  vertical motion need successful-damage proof against disabled baseline. Current
  and saved coherent pairs are considered, not continuous interpolation.
- Hardcoded3.2reachvsserverinteraction attribute/custom scale requires native/API
  decision if attribute-modifying modes are supported. No guessed new threshold.
- Aim look direction is current packet-handled rotation even for historical box;
  received rotation order vs client view is untrusted under stalls. Misses require
 4distincthandlingticks but that isn't evidence of4distinctclientclickticks.
- Raw geometry snapshots do not prove target identity on old recorder rows.
- Caller/thread API compilation, server lifecycle and shared CPU/GC remain gates.

51pure Java classes PASS once. Wiring tests updated to require UUID signature,
newSampleUUIDandall3validatedhistory paths, preserving formula/policy assertions.
Native FP1105/1107still need actual successful legal hits within10%baseline and
positive attack controls, not zero logs. Shared packet/riding geometry isn't
made safe merely by source/pure PASS.
