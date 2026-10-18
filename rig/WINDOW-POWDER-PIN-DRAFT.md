# DRAFT patch proposal: pinning the patch-1 values - window shape (U1) + powder jump band (U2)

Status: DRAFT for reviewer, 10-18 (standing queue item 1, after 0103/U7). U1 and U2 travel
together: both are patch-1 (78bcb3b) detection values gated on the SAME zero-FP battery and the
SAME GroundReplay trace grids, so one proposal covers both. Nothing here ships or changes code;
every number named is a PROPOSAL until its battery row is green. Log-only throughout.

## Problem

Patch 1 replaced the 3-consecutive buffer with a window and added the powder band, but the
values were never battery-pinned:

- **U1. Window shape** (Movement.java:18-19): GROUND_WINDOW=5, GROUND_FLAG_AT=3 - both already
  labeled PROPOSAL at the declaration. The window decides how much interleaved honesty erases
  spoof evidence; the threshold decides how much evidence fires.
- **U2. Powder jump band** (FpGuards.java: POWDER_JUMP_LO=0.60, POWDER_JUMP_HI=0.70,
  powderJumpBand()): the band exempts the vanilla struggle-climb kick (~0.63) near powder -
  too narrow is an FP, too wide is a hole (rig round 2: a hack big-jumped beside powder under
  the old blanket exemption, which is why this is a BAND, not an exemption).

## Pin path (no code in this patch; the battery + grids ARE the pin)

U1:
1. Battery zero-FP rows: GC01 (sneak-edge oscillation, 150 cycles x 2s = 300s bound, leg-log +
   wall-clock receipts - pre-committed: a SINGLE nofall flag kills the 5/3 proposal and goes
   back to the reviewer with the gndclaim trace lines), GC02 (slab/stair auto-step), GC05
   (powder struggle - also feeds U2). Zero nofall SIGNAL on each.
2. GroundReplay (w,t) grid over the SAME traces: the shipped (5,3) must sit inside a plateau of
   zero-legit-flag configs, not on an edge; a single-point plateau kills the values.
3. Catch side: GS-A/GS-C rows must still flag at (5,3) - a window that never fires is not a
   window. GS-C is respec'd true in-window 3-of-5 (patch 0024); GS-C2 (diluted contact) is
   preregistered below-threshold.

U2:
1. GC05 measured struggle dy values (JB I and JB II phases, no-boots verifications) REPORTED,
   never silently widened: every measured vanilla struggle dy must sit INSIDE [0.60, 0.70].
2. A measured value outside the band routes to rig/POWDER-EXEMPT-DRAFT.md (U6) - the band is
   not stretched to fit; the exempt-gate draft takes over.
3. Cheat side: the rig round-2 beside-powder big-jump row must still flag (the band does not
   cover dy > 0.70 or dy away from powder).

## Pre-committed kill criteria

- Any nofall flag on GC01/GC02/GC05 kills (5,3) - back to reviewer with trace lines.
- A single-point plateau on the (w,t) grid kills (5,3) (no margin).
- Any GC05 measured struggle dy outside [0.60, 0.70] kills the band as shipped; route to U6.
- Any beside-powder big-jump row going silent kills the band as shipped (hole).

## Order and dependencies

Battery run (shared with U3/U4 evidence rows) -> GroundReplay grids -> reviewer pins (5,3) and
[0.60, 0.70] as values (this proposal then becomes the citation). Any VALUE CHANGE after
pinning is a separate reviewed patch. Independent of the grace-decay Stage-2 gate and the U7
lane; U6 stays conditional on GC05 actually flagging.
