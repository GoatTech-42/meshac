# Static QA: patches 6-7 integration vs patches 1-5 (REFRESH 2026-10-18, patch 0102)

Supersedes the patch-0020 version of this file (verified against post-0019 HEAD). Refresh reason:
0022-0028 wired the Stage-1 decay shadow into Movement.java and added gndreset captures, and
0055/0061 added SIGNAL-SUPPRESSED receipts - three 0020 findings are stale. All checks below were
re-run scripted against HEAD (0101, a2f1760); results inline. Boss order 10-18 item (b).

## Patch 6 (GroundEvidence.decay + GroundReplay decay table) vs patch 1 (the window)

- PASS - ring layout (unchanged from 0020): bit 0 = newest claim; decay() scans oldest-first and
  forgives exactly the OLDEST unsupported claim (GroundEvidence.java; pinned by GroundEvidenceTest
  + D1-D4 in GroundReplayTest).
- SUPERSEDED (by design) - "no production caller": decay() now has exactly ONE production call
  site, Movement.java:338 `if (TRACE) s.gndEvDecay.decay();` - the Stage-1 shadow (0022/0026),
  TRACE-gated, acting ONLY on the shadow window. The live window full-clears on the same line
  (`s.gndEv.clear();`). Scripted: `rg -n '\.decay\(\)' src/main/java` -> exactly 1 hit.
- PASS - live path untouched: every `s.gndEv.clear()` site is one of the three full resets
  (:83 blink-setback, :344 soft-return, :346 post-flag) or the :338 exempt/grace boundary; no
  site calls decay() on gndEv.
- PASS - claim-set parity (0022 shadow wiring): the claim gate `if (!claimTol)` (Movement.java:322)
  wraps BOTH `s.gndEv.add(claimAir)` (:323) and the TRACE-gated `s.gndEvDecay.add(claimAir)` -
  identical claim set, order and config (both windows constructed with GROUND_WINDOW /
  GROUND_FLAG_AT, Movement.java:25). Classify-mode tolerated claims stay out of BOTH windows.
- PASS - capture contract: GroundReplay CLAIM/CLEAR/RESET patterns match the emit formats:
  gndclaim (:329, with count=), gndclear (:338, with decayCount=), gndreset (:83/:344/:346,
  cause + count + decayCount). gndreset is emitted at ALL THREE full-reset sites, and each site
  also clears gndEvDecay - per-site coverage pinned by DecayShadowWitnessTest.
- RETIRED - the 0020 known limitation "own-teleport/setback/post-flag clears NOT captured":
  0025/0028 added the gndreset capture at every full reset with both windows' pre-clear counts;
  BATTERY-SHADOW-DIVERGENCE-PLAN retires the old "counts not presence" reading. Decay scoring no
  longer treats reset spans as if the window lived on - K4 fails closed on any mismatch.
- UNCHANGED - patch 6 changes no detection output: no Verdict/MonitorGate/damage path touched;
  promotion of decay to the live window is a separate reviewed patch (Stage 2, gated on the
  Stage-1 battery).

## Patch 7 (grader TEXT2CHECK, all 23 checks) vs the patch 1-5 emit side (and later additions)

- PASS - every `hit = "..."` first word in Movement.java binds to a known check: air, fly, high,
  jump, ladder, levitation, microhop, nofall, speed, step, timer (11/11, scripted sweep).
- PASS - every direct Verdict.signal check name binds: antiknockback, blink, boatfly, glide,
  interact, inventory, mining, movement, noclip, nofall (scripted sweep over src/main/java).
- PASS - drift scan green (test-signal-checks): CHECKS == CheckNames.ALL (23), TEXT2CHECK covers
  each exactly once; the emit side is a strict subset.
- PASS - SIGNAL format unchanged: `[meshac] SIGNAL <player> <check> <detail> heat=N -> <step>`
  (Verdict.java:59 monitor branch, :110 signalLogOnly). The structured check field the grader
  binds on is stable.
- PASS (new since 0020) - SIGNAL-SUPPRESSED receipts (0055/0061, Verdict.java:67
  `[meshac] SIGNAL-SUPPRESSED {} {} {} count={} ({})`) do NOT bind: the SIGNAL regex requires
  "SIGNAL " + whitespace; scripted non-match + pinned by a 0101 grader fixture (one-shape
  contract: suppressed counts travel only via the receipt).
- PASS (new since 0020) - the 0022 telemetry lines never bind, scripted + 0101 fixtures:
  `[trace] nofall decay-shadow ...` (contains the word nofall - still no match), `[trace] nofall
  strong-arm ...`, `[trace] nofall shadow-evidence ...`, gndclaim/gndclear/gndreset: all None.
- PASS (new since 0020) - producer parity: pb-controls.py:177 keeps result['signals'] to lines
  containing "SIGNAL <player>" (excludes SUPPRESSED + all [trace] lines); 0101 adds the sibling
  'shadowSignals' field for the Stage-1 battery rows, and pb_grade grades them via
  expectShadow/shadowZero without touching the strict any-entry-on-a-zero-row FAIL rule.

## Verdict

Patches 6-7 integrate cleanly with 1-5 at source level as of HEAD (0101). The only production-code
interaction patch 6 has gained since 0020 is the reviewed Stage-1 TRACE-gated shadow call; the
live detection path is byte-identical to the patch 1-5 behavior. Honest limits: static reading
only - no javac in this sandbox (GroundReplay/GroundEvidence compile-verified only via Box's JDK
runs), no rig capture exists yet (battery never run), and grader fixtures are synthetic.
