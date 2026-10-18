# DRAFT patch proposal: grace-decay promotion (Stage 1 shadow: log-only, default off)

Status: DRAFT for reviewer, 10-17 (wording pass 2, per reviewer notes on 0021). Ordered by the
boss queue after patch 0020. Gated on battery D-row evidence (P3-FOLLOWUPS). Stage 1 changes no
verdict and is off by default; Stage 2 is NOT log-only and is scoped explicitly below.

## Problem

Every exempt/grace ground claim FULL-CLEARS the nofall evidence window (Movement.java:336). A
spoofer that re-arms grace by self-damage (fall/anvil/flyIntoWall buy 20 ticks of soft grace) or
dips through a medium keeps buying a fresh window - the known grace-farming hole (rebind delta 2;
GS-D/GS-E in REDTEAM-GROUNDSPOOF-PLAN are BY-DESIGN misses because of it).

## Candidate (patch 6, unreviewed, no production caller)

GroundEvidence.decay(): a clear forgives only the OLDEST unsupported claim. D-row fixtures
(patch 0020) pin the semantics: D1 shows the live-vs-decay divergence on a grace-farm stream,
D2/D3 show legit streams stay sub-flag, D4 pins one-claim forgiveness per boundary.

## Stage 1 (this patch): shadow dual-window - log-only, default off

Alongside the live gndEv, a shadow gndEvDecay replays the SAME claim set, ordering and threshold
(same single claim site, same GROUND_WINDOW/GROUND_FLAG_AT config). At every exempt/grace ground
claim the live window clear()s (unchanged) and the shadow decay()s. The shadow NEVER flags a
verdict, never calls Verdict/MonitorGate, never damages. At every full-reset site (own-teleport,
soft-mode return, post-flag reset) the shadow is cleared alongside the live window. When the
shadow is flagged while the live window is not, one rate-limited log-only line:

    [trace] nofall decay-shadow <name> count=N (grace-boundary persistence draft, log-only)

mirroring the O3 shadow pattern (patch 3, reviewed shape). Every shadow statement sits behind
TRACE (the MESHAC_TRACE env switch, unset by default): the shadow is OFF BY DEFAULT and
collects nothing when the env is unset.

Witness pins: GroundEvidenceTest decay pins (exist), D-row fixtures (exist, 0020),
sim/DecayShadowWitnessTest (new, this patch): asserts the shadow is declared with the live
config, adds claims at exactly the live window's single claim site, decays at exactly one site
(the exempt/grace reset), is cleared at every full-reset site, is TRACE-gated, and that NO shadow
line touches a Verdict/MonitorGate/causeFallDamage/hit-assignment/armNoFall/setback-return path.

## Stage 2 (separate, later, reviewer-gated): live swap - exactly what it can and cannot do

Stage 2 is a live behavior change, explicitly NOT log-only. It may do exactly ONE thing: behind
MESHAC_GROUND_DECAY=1 (unset by default; classify-mode env precedent, patch 0018), the
exempt/grace reset calls decay() instead of clear() on the live window. With the env unset, live
behavior is byte-identical to today.

Stage 2 may NOT: add, move or remove any Verdict/MonitorGate/causeFallDamage call; change any
threshold (GROUND_WINDOW, GROUND_FLAG_AT, STRONG_ARM_AT) or tolerance; change claim ordering or
soft-grace handling; arm, enable or alter any action. Every setback, NoFall arming and damage
call stays on its current acts()/MonitorGate path, exactly as configured.

Gate (unchanged): Stage-1 battery shows (a) decay-shadow divergence on the cheat rows
(GS-D/GS-E) and (b) ZERO decay-shadow flags on every legit row; then the same battery reruns on
the swapped jar before any default-on talk.

## Pre-committed kill criteria

- ANY decay-shadow flag on ANY legit battery row kills the promotion (FP-free gate outranks).
- GRACE-DECAY-FP-REVIEW bound stands: viable only at flagAt >= 3; if the battery moves flagAt
  below 3 the decay candidate is dead on arrival and this proposal is withdrawn.
- Uncaptured-clear conservatism (teleport/setback/post-flag) is re-stated in the battery report;
  a Stage-1 divergence explainable ONLY by an uncaptured clear is not evidence for Stage 2.

## Stage-1 evidence plan

rig/BATTERY-SHADOW-DIVERGENCE-PLAN.md (this patch): the rows (GS-D, GS-E + legit dip/knockback/
medium-exit rows), the expected shadow behavior per row, the GroundReplay cross-check, and the
light-job/ilhsmc-priority constraints. Box compiles the jar; the rig runs the rows.
