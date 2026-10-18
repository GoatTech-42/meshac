# DRAFT proposal: pinning the remaining unpinned behaviors (10-17)

Status: DRAFT for reviewer. Every behavior below is live-relevant but currently a PROPOSAL;
for each: what pins it, in what order, and what kills it. Nothing here ships or re-arms
enforcement; the pin path is battery + fixtures, log-only throughout.

## U1. Window shape: GROUND_WINDOW=5, GROUND_FLAG_AT=3 (patch 1, PROPOSAL)
- Pinned by: the zero-FP battery (GC01/GC02/GC05 zero-flag) + GroundReplay tuning tables over the
  SAME traces (the (w,t) grid already prints per-config flags) + the GS-A/GS-C catch rows.
- Order: battery first; table confirms the shipped (5,3) sits inside a plateau, not on an edge.
- Kills it: any legit-row flag (pre-committed), or the plateau being a single point.

## U2. Powder jump band 0.60-0.70 (patch 1, PROPOSAL)
- Pinned by: GC05 measured struggle dy values (report, never widen silently) + GS-F only if
  POWDER-EXEMPT-DRAFT ever lands.
- Kills it: GC05 dy measurements outside the band; a flag routes to the powder-exempt draft.

## U3. STRONG_ARM_AT=2 + the P3-strong handback (patch 4, shadow only)
- Pinned by: battery K1 (arm level vs legit maxCount), K2 (ZERO legit WOULD-fire - a wrong
  handback deals real damage), K3 (every flagged cheat F-row also strong-arms), K4 (fail-closed
  jar-state agreement, 0028/0029).
- Enforcement is a SEPARATE later patch, gated on all four; this proposal does not request it.

## U4. MESHAC_GROUND_TOL=classify default-on (patch 3, env-gated)
- Pinned by: battery would-have-passed rates (classify-mode rows) showing tolerated partial-shape
  claims are never attack-relevant; reviewer resubmission before any default flip.
- Kills it: any cheat row caught by raw but missed under classify.

## U5. Grace-decay promotion (patch 6 candidate; Stage-1 shadow in 0022+)
- Path: GRACE-DECAY-PROMOTION-DRAFT.md (Stage 1 shadow now wired, default off;
  BATTERY-SHADOW-DIVERGENCE-PLAN.md produces the evidence; Stage 2 env-gated swap gated on it).
- Kills it: pre-committed kill criteria (any legit shadow flag; flagAt<3; K4 mismatch).

## U6. Powder-exempt gate entry (draft only)
- Path: rig/POWDER-EXEMPT-DRAFT.md (variant A frozen-window preferred), gated on GC05; no code.

## U7. Microhop buffer (bufHop>=3, hopClock 30) + KB window (noKb/450ms)
- Pinned by: the microhop/antiknockback rows (nb-microhop-legit ceiling guard, nb-kb-*) with the
  attack side honestly blocked until meadow MicroHop lands (patch 0017 spec); GS-C2 prices the
  landing-interleave hole once a driver exists.

## Order and dependencies
Battery (U1-U4 evidence) -> GroundReplay tables -> reviewer pins U1/U2 values -> U3 enforcement
patch (separate) and U4 default flip (separate) -> U5 Stage 2 on its own evidence -> U6 only if
GC05 flags. U7 rides the meadow module queue, not the battery.
