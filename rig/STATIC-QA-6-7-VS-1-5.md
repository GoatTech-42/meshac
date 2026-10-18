# Static QA: patches 6-7 integration vs patches 1-5 (2026-10-17, patch 0020)

All checks run against bundle HEAD (post-0019). Scripted; results reproduced inline.

## Patch 6 (GroundEvidence.decay + GroundReplay decay table) vs patch 1 (the window)

- PASS - ring layout: GroundEvidence documents bit 0 = newest claim; decay() scans oldest-first
  (`for (int i = filled - 1; i >= 0; i--)`) and forgives exactly the OLDEST unsupported claim.
  Semantics pinned in GroundEvidenceTest + the D-row fixtures (D1-D4, GroundReplayTest, patch 0020).
- PASS - no production caller: `grep -rn '\.decay(' src/main/java/` is EMPTY (escaped grep; an
  unescaped first pass matched the definition site itself - script artifact, not a caller). The
  live path still full-clears; promotion is a separate reviewed patch.
- PASS - capture contract (patch 4 lines vs patch 6 harness): GroundReplay's CLAIM regex matches
  the emitted `[trace] gndclaim <name> air=<bool>` (Movement.java:327) and CLEAR matches
  `[trace] gndclear <name>` (:336); the patch-4 shadow lines ("nofall strong-arm",
  "strong-handback WOULD fire") do NOT collide with either parse.
- KNOWN AND DOCUMENTED (not a regression): own-teleport (:83), setback, and post-flag clears are
  NOT captured by gndclear; decay scoring treats those spans as if the window lived on -
  conservative for FP reading, recorded in GRACE-DECAY-FP-REVIEW.md.

## Patch 7 (grader TEXT2CHECK, all 23 checks) vs the patch 1-5 emit side

- PASS - every `hit = "..."` first word in Movement.java binds to a known check:
  air, fly, high, jump, ladder, levitation, microhop, nofall, speed, step, timer (11).
- PASS - every direct Verdict.signal check name binds: antiknockback, blink, boatfly, glide,
  interact, inventory, mining, movement, noclip, nofall (10).
- PASS - drift scan (test-signal-checks): CHECKS == CheckNames.ALL (23) and TEXT2CHECK covers each
  exactly once; the emit side above is a strict subset.
- Verdict format is unchanged by patches 1-5 (`SIGNAL <player> <check> <detail> heat=N -> <step>`),
  so the structured check field the grader binds on is stable across the lineage.

## Verdict

Patches 6-7 integrate cleanly with 1-5 at source level. No enforcement-adjacent line found
(decay is dead code by design; patch 7 is rig tooling). Remaining honest caveat: GroundReplay
(patches 4/6/0014 additions) is compile-verified only as of Box's JDK run - this sandbox has no
javac; the python-side pins are all green.
