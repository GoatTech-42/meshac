# meshac ground-spoof stack: promotion readiness (patches 1-9, c4b3fe8 line)

One page: what each patch changes, what actually ships in the jar's detection output, what is
measurement-only, and the gate in front of each. All work is rig-only, log-only, no live deploy;
nothing here removes or damages a player (the server runs monitor/log posture throughout).

| # | commit | what it is | detection output | gate |
|---|--------|-----------|------------------|------|
| 1 | 78bcb3b | windowed nofall evidence (3-of-5 replaces 3-consecutive), spoof-claim chain-reset gate, powder jump band 0.60-0.70 | CHANGES what flags | GO (reviewer 5:16:58pm, relayed via parent 10-17 7:42pm): monitor/log-only rig round-2, CONDITIONAL on the GC01 sneak-edge + GC05 JB-powder rows sitting in the zero-FP battery before any enforcement-adjacent reading; window/threshold/band stay PROPOSALS until that battery is green |
| 2/3 | e03f802 | O3 soft-grace shadow evidence; MESHAC_GROUND_TOL=classify keeps tolerated partial-shape claims out of the window | shadow: log only, never flags; classify: changes detection ONLY with the env var set (default off) | battery would-have-passed rates before classify is default-on; resubmit clean GO (reviewer, relayed via parent 10-17 7:42pm). Patch 2 (first cut) was the NO-GO and is dead |
| 4/5 | ceb2540, 386448d | P3-strong shadow handback arm (count>=2) + WOULD-fire landing watcher + gndclaim/gndclear capture + GroundReplay harness | log only; never flags, never damages, MonitorGate untouched | CONDITIONAL GO (reviewer via parent, 10-17 7:39pm): two fixes required before the battery run - FIX 1 (clearPendingActions clears the shadow arm + witness pin) landed as patch 5 / 386448d (Movement.java:40, MonitorSourceWitnessTest); FIX 2 (K4 wording, doc-level) landed as patch 0014: K4 = per-player maxCount equality + flag presence/absence + first-arm presence, no edge counts (the jar latches; GroundReplay prints a K4 section). GO now UNCONDITIONAL (reviewer, relayed via parent 10-17 7:42pm). Shadow support probe VERIFIED identical to the real nfAt watcher probe (Movement.java:372 tb vs :383 sb, same box expression; pinned in test-signal-checks). Then: battery run -> K1-K4 (P3-STRONG-DRAFT.md) -> SEPARATE enforcement patch |
| 6 | 126fac1 | GroundEvidence.decay() + GroundReplay decay-on-clear table | measurement only; decay() has NO production call site | battery D-rows; viable only at flagAt>=3 (GRACE-DECAY-FP-REVIEW.md) |
| 7 | 392a5f4 | grader TEXT2CHECK: all 23 checks bind by the structured check field; per-row 'checks' scoping | rig tooling only, no jar change | reviewer; stricter grading only (off-target signals now INVALID) |
| 8 | 0f88e21 | capture-row specs for fly/speed/combat/mining/interact/inventory fixtures | rig tooling/docs only | Box rig time; needs click/holdmouse leg verbs in the driver |
| 9 | 63656cf | grace-decay FP hardening review + consecutive-clear pin | docs/tests only | none |

## FP risks, consolidated

- Every detection change (patch 1 window/band, classify) is a PROPOSAL gated on the zero-FP
  battery; the battery doc carries the rows and the reviewer nits (idle row, pass bar, config
  binding, B2 rebind).
- Decay scoring: conservative for FP by construction (over-counts on uncaptured
  teleport/setback/post-flag clears) but can OVER-PROMISE cheat catch - read decay-table wins
  with the discount in GRACE-DECAY-FP-REVIEW.md before quoting them.
- P3-strong: K1 (arm level vs legit max count), K2 (any legit WOULD-fire kills it - a wrong
  handback deals real fall damage), K3 (every flagged cheat F-row must also show a strong-arm),
  K4 (harness vs jar agreement, per player: maxCount equality + flag presence/absence + first-arm
  presence - the jar latches; edge counts are tuning signal only, NOT the agreement metric).
  All pre-committed before the battery.

## Outstanding before any of this promotes

1. Box: battery run on the patch-3+4 jar with TRACE on (rows in the battery doc).
2. Reviewer: patches 3 and 4/5 GO (relayed via parent 10-17 7:42pm); 0011, 0012, 0013 static GO
   (relayed 10-17 8:17pm). Verdicts outstanding on 6, 7, 8, 9, 0010, 0014, 0015, 0016, 0017, 0018.
   Patches 6-9 are NOT reviewed - never cite them as surviving.
3. GroundReplay over the battery traces -> arm level + decay viability from the score tables.
4. Only then: the separate, separately-reviewed enforcement patches (P3-strong wiring,
   classify default-on, grace-decay policy, O2 per the lead's ordering).

Verified at stack HEAD 63656cf: full pure suite (~90 mains) green; pb_grade/signal_checks/
newbounds python suites green. Not verified: no battery data exists yet; nothing ran live.
