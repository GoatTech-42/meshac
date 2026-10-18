# Mutation re-derivation on tip 0105 (standing queue item 2, 2026-10-18)

## Carry-over finding (primary)

Box's mutation 7/7 ran green on 785bbe1 (0095 tip). `git diff --stat 785bbe1..HEAD -- src/ sim/`
is EMPTY (0096-0105 touched rig/*.py, rig/*.sh and rig/*.md only), so the Java mutation surface
is byte-identical to the last green run. HONEST STATUS: that is a SOURCE-DIFF reading, not a carried
result - claiming 7/7 for the current tip requires the actual old run receipts AND the same harness,
toolchain and config; none of that is re-verified here. Until Box re-runs (or produces the receipts),
the carryover is an expectation, not evidence.

## Derived 7-target manifest (anchors verified on tip 4eace9a)

If Box's harness retargets, these are the 7 highest-value mutation targets from the 0079-0094
Ladder/policy work, each with the exact tip anchor, the flip, and the catching assertion
(all in sim/LadderTierATest.java unless noted; line refs = current tip):

| # | Anchor (tip) | Mutation | Caught by |
|---|--------------|----------|-----------|
| M1 | Ladder.java:53 `if (invalid) p = p.sanitized();` | drop the sanitize (use raw p) | :109 - sanitized-invalid params must behave exactly like the defaults |
| M2 | Ladder.java:72 `if (invalid) return Step.LOG;` | delete the early LOG return | :117 - raw invalid params must record evidence but return LOG (twice) |
| M3 | Ladder.java:48 `now(...)`: `!bound \|\| !p.valid() \|\| epochStale(p, false)` | drop `!p.valid()` | :120 - invalid params read as nothing |
| M4 | Ladder.java:54 `if (epochStale(p, invalid)) reset();` | delete the reset | :119, :140-141 - invalid->valid epoch regression + params-change reset |
| M5 | Ladder.java:79 `tierAClusters >= p.minClustersTierA()` | hardcode 2 (the 0079 #1 regression) | :64-68 - requireClustersTierA 4 bars/withholds |
| M6 | Ladder.java:96-104 `Math.max(0, a)` clamp in effectiveOffences | drop the clamp | :97-98 - future-dated case counts fresh, per-case isolation |
| M7 | Ladder.java:110 `if (!Double.isFinite(effective) \|\| effective < 0) return 1;` | delete the fail-closed guard | :113 - corrupt effective offences are the kick rung, no skip benefit |

Alternates if Box's 7 differ: Ladder.java:111-112 saturation cast (caught :114);
Ladder.java:87 corrupt-lastValid ignore (caught :128); Ladder.java:106-107 bad-half-life fold
into `badP || badHl` (caught :127); Config reload-ordering mutations (caught by
MonitorSourceWitnessTest reload witnesses, re-anchored 0095).

## Caveats

- This manifest is DERIVED, not confirmed: Box owns the actual harness target list. Diff its 7
  against this table; any target of Box's not listed here should be added with its catching
  assertion named (a target without a named catcher is a guess, not a mutation).
- A mutation the suite does NOT catch is a finding, not a pass: report the survivor, do not
  retune the mutation.
- No javac in this sandbox: anchors are source-verified only; the flips themselves are Box's run.
