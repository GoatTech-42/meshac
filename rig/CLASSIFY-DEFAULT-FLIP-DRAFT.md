# DRAFT patch proposal: MESHAC_GROUND_TOL=classify default-on (U4)

Status: DRAFT for reviewer, 10-18 (standing queue, 0105 = U4 per parent instruction). U4 is the
last unpinned behavior in UNPINNED-BEHAVIORS-PROPOSAL.md without its own proposal doc. This
proposal covers ONLY the default flip; the classify mode itself is patch 2/3 (e03f802),
reviewed shape. Nothing ships from this doc; the flip is a separate reviewed patch after the
evidence exists. Log-only until then.

## What classify does today

With MESHAC_GROUND_TOL=classify set, a tolerated ground claim (a landing on a partial shape -
snow layer, carpet, fence/wall/closed-gate family) NEITHER enters NOR evicts the nofall evidence
window (Movement.java:322 `if (!claimTol)` gates both the live and the decay-shadow window).
Default is OFF: tolerated claims currently count like any other claim. Support derivation scans
the blocks whose collision shapes reach the 0.13 strip (e03f802 resubmit; shape bounding boxes,
not voxel-exact - noted there).

## Why default-on is still a proposal

The flip changes what counts as evidence for every player by default. It is safe only if
tolerated partial-shape claims are never attack-relevant - a claim a spoofer could ride. That
is an empirical statement, not a source-level one, so the pin is battery data, not argument.

## Pin path

1. Battery: the would-have-passed rates on classify-mode rows - every battery capture records,
   per row, the claims classify would have dropped; the rate must show tolerated claims are
   never adjacent to a real spoof stream (report per row, not aggregate).
2. Kill criterion (pre-committed, from U4): ANY cheat row caught by raw mode but MISSED under
   classify kills the flip - classify stays env-gated forever, and the row goes back to the
   reviewer with the capture.
3. The zero-FP side rides the existing rows (GC01/GC02/GC05 and family): classify must produce
   ZERO nofall SIGNALs on every legit row (same bar as raw).
4. Reviewer resubmission before any default flip (U4, unchanged): the flip patch carries the
   battery table, the per-row would-have-passed rates, and the kill-criterion outcome.

## What the flip patch will contain (when gated green)

Exactly one behavior change: the default of MESHAC_GROUND_TOL when the env var is unset, plus
the witness/test updates that pin the new default. Explicitly NOT: any threshold, window, band,
support-derivation, or ordering change; the env var keeps working (raw mode remains selectable
for diagnosis).

## Kill criteria (restated, pre-committed)

- Any cheat row caught raw but missed under classify: dead.
- Any new legit-row flag under classify that raw does not produce: dead.
- Would-have-passed rates showing tolerated claims adjacent to spoof streams on any row: dead.

## Order and dependencies

Battery run (shared rows with U1/U2/U3 evidence) -> would-have-passed rate table -> reviewer
resubmission -> the flip patch. Independent of U5 Stage 2 and the U7 lane; GroundReplay grids
are not part of this pin (classify gates claim ENTRY, upstream of the window).
