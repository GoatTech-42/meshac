# DRAFT patch proposal: grace-decay promotion (shadow first, log-only throughout)

Status: DRAFT for reviewer, 10-17. Ordered by the boss queue after patch 0020. Gated on battery
D-row evidence (P3-FOLLOWUPS). Nothing here changes a verdict today.

## Problem

Every exempt/grace ground claim FULL-CLEARS the nofall evidence window (Movement.java:336). A
spoofer that re-arms grace by self-damage (fall/anvil/flyIntoWall buy 20 ticks of soft grace) or
dips through a medium keeps buying a fresh window - the known grace-farming hole (rebind delta 2;
GS-D/GS-E in REDTEAM-GROUNDSPOOF-PLAN are BY-DESIGN misses because of it).

## Candidate (patch 6, unreviewed, no production caller)

GroundEvidence.decay(): a clear forgives only the OLDEST unsupported claim. D-row fixtures
(patch 0020) pin the semantics: D1 shows the live-vs-decay divergence on a grace-farm stream,
D2/D3 show legit streams stay sub-flag, D4 pins one-claim forgiveness per boundary.

## Proposal: two stages, both log-only

STAGE 1 (this patch): shadow dual-window. Alongside the live gndEv, run gndEvDecay through the
same claims; at every exempt/grace ground claim the live window clear()s (unchanged) and the
shadow decay()s. The shadow NEVER flags, never touches MonitorGate, never damages. When the
shadow is flagged while the live window is not, emit a rate-limited trace line:

    [trace] nofall decay-shadow <name> count=N (grace-boundary persistence draft, log-only)

mirroring the O3 shadow pattern (patch 3, reviewed shape). Witness pins: GroundEvidenceTest decay
pins (exist), D-row fixtures (exist, 0020), a MonitorSourceWitnessTest-style source pin asserting
the shadow wiring never calls Verdict/MonitorGate, and a clearPendingActions-style reset pin
(shadow cleared at every full-reset site, patch-5 FIX-1 pattern).

STAGE 2 (separate, later, reviewer-gated): env-gated live swap (MESHAC_GROUND_DECAY=1, default
off, classify-mode precedent): the exempt/grace reset calls decay() instead of clear() on the
LIVE window. Only after Stage-1 battery data shows (a) decay-shadow divergence on the cheat
D-rows (GS-D/GS-E) and (b) ZERO decay-shadow flags on every legit row. Then the same battery
reruns on the swapped jar before any default-on talk.

## Pre-committed kill criteria

- ANY decay-shadow flag on ANY legit battery row kills the promotion (FP-free gate outranks).
- GRACE-DECAY-FP-REVIEW bound stands: viable only at flagAt >= 3; if the battery moves flagAt
  below 3 the decay candidate is dead on arrival and this proposal is withdrawn.
- Uncaptured-clear conservatism (teleport/setback/post-flag) is re-stated in the battery report;
  a Stage-1 divergence explainable ONLY by an uncaptured clear is not evidence for Stage 2.

## What Box runs for Stage 1 evidence

The D-row battery rows (GS-D grace-farm, GS-E medium dips + the legit D-family) with TRACE on;
GroundReplay over gndclaim/gndclear scores both policies offline as today, and the shadow trace
line is the in-jar cross-check of the same divergence (K4 presence semantics).
